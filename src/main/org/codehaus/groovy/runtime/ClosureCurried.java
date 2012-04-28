package org.codehaus.groovy.runtime;

import groovy.lang.Closure;

public final class ClosureCurried extends Closure {

    private final Closure original;

    public ClosureCurried(final Closure origClosure) {
        super(origClosure.getOwner(), origClosure.getDelegate());
        original = origClosure;
    }

    @Override
    public Object call() {
        return original.call();
    }

    @Override
    public Object call(Object arg) {
        return original.getMaximumNumberOfParameters() < 2 ?
            original.call(arg) :
            new ClosureCurried(original.curry(arg));
    }

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
