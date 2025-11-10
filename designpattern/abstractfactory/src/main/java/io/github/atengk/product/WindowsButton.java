package io.github.atengk.product;

/**
 * 具体产品：Windows 风格按钮
 */
public class WindowsButton implements Button {
    @Override
    public void display() {
        System.out.println("显示 Windows 风格的按钮");
    }
}
