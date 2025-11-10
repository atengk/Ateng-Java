package io.github.atengk.controller;

import io.github.atengk.service.memento.Caretaker;
import io.github.atengk.service.memento.Editor;
import io.github.atengk.service.memento.EditorMemento;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 备忘录模式控制器演示
 */
@RestController
public class MementoController {

    private final Editor editor;
    private final Caretaker caretaker;

    public MementoController(Editor editor, Caretaker caretaker) {
        this.editor = editor;
        this.caretaker = caretaker;
    }

    /**
     * 写入内容接口
     */
    @GetMapping("/memento/write")
    public String write(@RequestParam String text) {
        editor.write(text);
        return "写入完成";
    }

    /**
     * 保存当前状态接口
     */
    @GetMapping("/memento/save")
    public String save() {
        EditorMemento memento = editor.save();
        caretaker.addMemento(memento);
        return "状态已保存，当前版本：" + (caretaker.size() - 1);
    }

    /**
     * 恢复指定版本状态接口
     */
    @GetMapping("/memento/restore")
    public String restore(@RequestParam int version) {
        EditorMemento memento = caretaker.getMemento(version);
        if (memento != null) {
            editor.restore(memento);
            return "已恢复到版本：" + version;
        } else {
            return "版本不存在";
        }
    }

    /**
     * 查看当前内容
     */
    @GetMapping("/memento/content")
    public String content() {
        return editor.getContent();
    }
}
