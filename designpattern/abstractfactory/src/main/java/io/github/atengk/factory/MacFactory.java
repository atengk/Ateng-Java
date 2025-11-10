package io.github.atengk.factory;

import io.github.atengk.product.*;

/**
 * 具体工厂：Mac 风格工厂
 */
public class MacFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}