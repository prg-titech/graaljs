/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.LargeInteger;

/**
 * The Left Shift Operator ( << ), special-cased for the step to be a constant integer value.
 */
@NodeInfo(shortName = "<<")
public abstract class JSLeftShiftConstantNode extends JSUnaryNode {

    protected final int shiftValue;

    protected JSLeftShiftConstantNode(JavaScriptNode operand, int shiftValue) {
        super(operand);
        this.shiftValue = shiftValue;
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        assert right instanceof JSConstantIntegerNode;
        int shiftValue = ((JSConstantIntegerNode) right).executeInt(null);
        if (left instanceof JSConstantIntegerNode) {
            int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
            return JSConstantNode.createInt(leftValue << shiftValue);
        }
        Truncatable.truncate(left);
        return JSLeftShiftConstantNodeGen.create(left, shiftValue);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryExpressionTag.class)) {
            // need to call the generated factory directly to avoid constant optimizations
            JSConstantNode constantNode = JSConstantIntegerNode.create(shiftValue);
            JavaScriptNode node = JSLeftShiftNodeGen.create(getOperand(), constantNode);
            transferSourceSectionNoTags(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    public abstract int executeInt(int a);

    @Specialization
    protected int doInteger(int a) {
        return a << shiftValue;
    }

    @Specialization
    protected int doLargeInteger(LargeInteger a) {
        return a.intValue() << shiftValue;
    }

    @Specialization(replaces = "doInteger")
    protected int doGeneric(Object a,
                    @Cached("create()") JSToInt32Node leftInt32) {
        return leftInt32.executeInt(a) << shiftValue;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSLeftShiftConstantNodeGen.create(cloneUninitialized(getOperand()), shiftValue);
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " << " + shiftValue + ")";
        }
        return null;
    }
}
