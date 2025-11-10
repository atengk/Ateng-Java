package io.github.atengk.product;

/**
 * 具体产品：Mac 风格按钮
 */
public class MacButton implements Button {
    @Override
    public void display() {
        System.out.println("显示 Mac 风格的按钮");
    }
}
