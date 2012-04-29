package org.codehaus.groovy.runtime;

import groovy.lang.Closure;

/**
 * ClosureCurried is a wrapper for <code>Closure</code> which transforms a
 * <code>Closure</code> instance that takes multiple arguments in such a way
 * that it can be called as a chain of closures, each with a single argument. <p>
 *
 * This class is normally only used internally, through the <code>curried()</code>
 * method on <code>Closure</code> object. <p>
 *
 * Example:
 * <pre>
 * def adder = { a, b, c -> a + b + c },
 *     addCurr = adder.curried()
 *
 * // fails with message: no signature of method doCall() is applicable...
 * // addCurr(2, 3) // ... accepts only one argument
 *
 * assert addCurr instanceof org.codehaus.groovy.runtime.ClosureCurried
 * def add2 = addCurr(2)
 * assert add2 instanceof org.codehaus.groovy.runtime.ClosureCurried
 * def add5 = add2(3)
 * assert add5 instanceof org.codehaus.groovy.runtime.ClosureCurried
 * assert add5(4) == 9
 * </pre>
 *
 * Curried closure has its dual in the form of <code>uncurried()</code>.
 * Since <code>uncurried()</code> is declared in this class, only
 * closures that were previously curried can be uncurried. <p>
 *
 * Example of uncurrying:
 * <pre>
 * def adder = { a, b, c -> a + b + c }
 *
 * // fails with message: No signature of method uncurried() is applicable...
 * // adder.uncurried() // ... needs to be curried first
 *
 * assert adder.curried()(2).uncurried()(3, 4) == 9
 * </pre>
 *
 * <em>Note:</em> this implementation should not be confused with
 * <code>org.codehaus.groovy.runtime.CurriedClosure</code> which
 * partially applies its arguments to a <code>Closure</code> instance.
 *
 * @author Dinko Srkoč
 */
public final class ClosureCurried extends Closure {

    private final Closure original;

    public ClosureCurried(final Closure origClosure) {
        super(origClosure.getOwner(), origClosure.getDelegate());
        original = origClosure;
    }

    /**
     * Delegates to the wrapped closure.
     * @return the result of invoking wrapped closure's <code>call()</code> method.
     */
    @Override
    public Object call() {
        return original.call();
    }

    /**
     * Delegates to the wrapped closure if that closure accepts less than two
     * parameters, otherwise returns new <code>ClosureCurried</code> with parameter
     * <code>arg</code> applied.
     *
     * @param arg argument that is applied to the closure
     * @return the value of the wrapped closure if that closure accepts less then
     *         two parameters, and new closure with <code>arg</code> applied
     *         otherwise
     */
    @Override
    public Object call(Object arg) {
        return original.getMaximumNumberOfParameters() < 2 ?
            original.call(arg) :
            new ClosureCurried(original.curry(arg));
    }

    /**
     * Uncurries a curried closure, i.e. returns a closure to its normal form.
     * <code>uncurried()</code> is a dual of <code>curried()</code>. <p>
     *
     * Note that Closure that is not <code>curried()</code> cannot be
     * <code>uncurried()</code>. This stems from the implementation:
     * <code>Closure#curried()</code> returns the instance of
     * <code>ClosureCurried</code> which implements the actual
     * <code>uncurried()</code> method:
     * <pre>
     * // ok
     * { a, b -> a + b }.curried().uncurried()
     *
     * // fails because curried() was not called on closure before uncurried()
     * { a -> { b -> a + b }}.uncurried()
     * </pre>
     *
     * Here are some examples:
     * <pre class="groovyTestCase">
     * def adder = { a, b, c, d -> a + b + c + d }
     * def sum = adder(1, 2, 3, 4)
     *
     * assert sum == adder.curried().uncurried()(1, 2, 3, 4)
     * assert sum == adder.curried()(1)(2).uncurried()(3, 4)
     * </pre>
     *
     * @return a <code>Closure</code> that accepts the rest of the parameters - those
     *         that are not already applied to <code>ClosureCurried</code>
     * @see Closure#curried()
     */
    public Closure uncurried() {
        // uncurried assumes original to be ClosureCurried for all but the innermost closure.
        // This is ensured inside call(Object).
        return original instanceof ClosureCurried ? ((ClosureCurried) original).uncurried() : original;
    }

    @Override
    public Object clone() {
        return new ClosureCurried((Closure) original.clone());
    }
}
