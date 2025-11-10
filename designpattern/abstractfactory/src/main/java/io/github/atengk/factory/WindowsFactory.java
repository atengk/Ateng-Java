package io.github.atengk.factory;


import io.github.atengk.product.Button;
import io.github.atengk.product.TextField;
import io.github.atengk.product.WindowsButton;
import io.github.atengk.product.WindowsTextField;

/**
 * 具体工厂：Windows 风格工厂
 */
public class WindowsFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}
