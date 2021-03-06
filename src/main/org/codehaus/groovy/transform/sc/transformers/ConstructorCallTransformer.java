/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.transform.sc.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import static org.codehaus.groovy.transform.stc.StaticTypesMarker.*;

public class ConstructorCallTransformer {
    private final StaticCompilationTransformer staticCompilationTransformer;

    public ConstructorCallTransformer(StaticCompilationTransformer staticCompilationTransformer) {
        this.staticCompilationTransformer = staticCompilationTransformer;
    }

    Expression transformConstructorCall(final ConstructorCallExpression expr) {
        ConstructorNode node = (ConstructorNode) expr.getNodeMetaData(DIRECT_METHOD_CALL_TARGET);
        if (node == null) return expr;
        if (node.getParameters().length == 1 && StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(node.getParameters()[0].getType(), ClassHelper.MAP_TYPE)) {
            Expression arguments = expr.getArguments();
            if (arguments instanceof TupleExpression) {
                TupleExpression tupleExpression = (TupleExpression) arguments;
                List<Expression> expressions = tupleExpression.getExpressions();
                if (expressions.size() == 1) {
                    Expression expression = expressions.get(0);
                    if (expression instanceof MapExpression) {
                        MapExpression map = (MapExpression) expression;
                        // check that the node doesn't belong to the list of declared constructors
                        ClassNode declaringClass = node.getDeclaringClass();
                        for (ConstructorNode constructorNode : declaringClass.getDeclaredConstructors()) {
                            if (constructorNode == node) {
                                return staticCompilationTransformer.superTransform(expr);
                            }
                        }
                        // replace this call with a call to <init>() + appropriate setters
                        // for example, foo(x:1, y:2) is replaced with:
                        // { def tmp = new Foo(); tmp.x = 1; tmp.y = 2; return tmp }()

                        VariableExpression vexp = new VariableExpression("obj" + System.currentTimeMillis(), declaringClass);
                        ConstructorNode cn = new ConstructorNode(Opcodes.ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, EmptyStatement.INSTANCE);
                        cn.setDeclaringClass(declaringClass);
                        ConstructorCallExpression call = new ConstructorCallExpression(declaringClass, new ArgumentListExpression());
                        call.putNodeMetaData(DIRECT_METHOD_CALL_TARGET, cn);
                        DeclarationExpression declaration = new DeclarationExpression(
                                vexp, Token.newSymbol("=", expr.getLineNumber(), expr.getColumnNumber()),
                                call
                        );
                        BlockStatement stmt = new BlockStatement();
                        stmt.addStatement(new ExpressionStatement(declaration));
                        for (MapEntryExpression entryExpression : map.getMapEntryExpressions()) {
                            int line = entryExpression.getLineNumber();
                            int col = entryExpression.getColumnNumber();
                            Expression keyExpression = staticCompilationTransformer.transform(entryExpression.getKeyExpression());
                            Expression valueExpression = staticCompilationTransformer.transform(entryExpression.getValueExpression());
                            BinaryExpression bexp = new BinaryExpression(
                                    new PropertyExpression(vexp, keyExpression),
                                    Token.newSymbol("=", line, col),
                                    valueExpression
                            );
                            bexp.setSourcePosition(entryExpression);
                            stmt.addStatement(new ExpressionStatement(bexp));
                        }
                        stmt.addStatement(new ReturnStatement(vexp));
                        ClosureExpression cl = new ClosureExpression(Parameter.EMPTY_ARRAY, stmt);
                        cl.setSourcePosition(call);
                        MethodCallExpression result = new MethodCallExpression(cl, "call", ArgumentListExpression.EMPTY_ARGUMENTS);
                        result.setMethodTarget(StaticTypeCheckingVisitor.CLOSURE_CALL_NO_ARG);
                        VariableScopeVisitor visitor = new VariableScopeVisitor(staticCompilationTransformer.getSourceUnit());
                        visitor.prepareVisit(staticCompilationTransformer.getClassNode());
                        visitor.visitClosureExpression(cl);
                        return result;
                    }
                }
            }

        }
        return staticCompilationTransformer.superTransform(expr);
    }
}