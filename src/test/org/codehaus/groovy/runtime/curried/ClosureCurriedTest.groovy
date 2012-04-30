package org.codehaus.groovy.runtime.curried

/**
 * Test for Closure's `curried` method.
 *
 * @author Dinko Srkoč
 */
class ClosureCurriedTest extends GroovyTestCase {

    Closure c1, c2, c3, c4, c5, c6

    void setUp() {
        c1 = { a, b, c -> a + b + c }
        c2 = { a, b, c -> a / b / c }
        c3 = { -> 1 }
        c4 = { a -> a + 1 }
        c5 = { a, b, c = 3 -> a + b + c }
        c6 = { a, b -> {-> a + b } }
    }

    void testCurried() {
        assert c1.curried()(1)(2)(3) == 6
        assert c2.curried()(8)(2)(5) == 0.8
        assert c1.curried(2)(3)(4) == 9
        assert c1.curried(2, 3)(4) == 9
        assert c1.curried(2, 3, 4)() == 9
        assert c3.curried()() == 1
        assert c4.curried()(2) == 3
        assert c4.curried(2)() == 3
        assert c5.curried()(1)(2)(4) == 7
        assert c5.curried()(1)(2)() == 6
        assert c5.curried(1)(2)(4) == 7
        assert c5.curried(1)(2)() == 6
        assert c6(1, 2)() == 3
        assert c6.curried()(1)(2)() == 3
        assert c6.curried(1)(2)() == 3
        assert c6.curried(1, 2)()() == 3
    }

    void testUncurried() {
        assert c1.curried().uncurried()(1, 2, 3) == 6
        assert c2.curried().uncurried()(8, 2, 5) == 0.8
        assert c1.curried(2).uncurried()(3, 4) == 9
        assert c1.curried(2, 3).uncurried()(4) == 9
        assert c1.curried(2, 3, 4).uncurried()() == 9
        assert c1.curried()(1).uncurried()(2, 3) == 6
        assert c1.curried()(1)(2).uncurried()(3) == 6
        assert c1.curried(1)(2).uncurried()(3) == 6
        assert c3.curried().uncurried()() == 1
        assert c4.curried().uncurried()(2) == 3
        assert c4.curried(2).uncurried()() == 3
        assert c5.curried().uncurried()(1, 2, 4) == 7
        assert c5.curried().uncurried()(1, 2) == 6
        assert c5.curried(1).uncurried()(2, 4) == 7
        assert c5.curried(1).uncurried()(2) == 6
        assert c6.curried().uncurried()(1, 2)() == 3
        assert c6.curried(1).uncurried()(2)() == 3
        assert c6.curried(1, 2).uncurried()()() == 3
    }

    void testIrregularCases() {
        shouldFail(MissingMethodException) { c1.uncurried() }
        shouldFail(IllegalArgumentException) { c1.curried(1, 2, 3, 4) }
        shouldFail(MissingMethodException) { c1.curried().uncurried()(1, 2, 3, 4) }
        shouldFail(MissingMethodException) { c1.curried()(1)(2)(3)(4) }
        shouldFail(MissingMethodException) { c1.curried(1)(2).uncurried(3, 4) }
    }
}
