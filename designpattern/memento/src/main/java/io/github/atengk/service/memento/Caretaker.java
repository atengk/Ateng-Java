package io.github.atengk.service.memento;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 备忘录管理者（Caretaker）
 */
@Component
public class Caretaker {

    private final List<EditorMemento> mementos = new ArrayList<>();

    /**
     * 添加备忘录
     */
    public void addMemento(EditorMemento memento) {
        mementos.add(memento);
    }

    /**
     * 获取指定版本的备忘录
     */
    public EditorMemento getMemento(int index) {
        if (index >= 0 && index < mementos.size()) {
            return mementos.get(index);
        }
        return null;
    }

    /**
     * 获取备忘录数量
     */
    public int size() {
        return mementos.size();
    }
}
