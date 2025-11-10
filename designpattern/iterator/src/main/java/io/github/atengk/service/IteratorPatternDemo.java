package io.github.atengk.service;


import io.github.atengk.aggregate.ProductCollectionImpl;
import io.github.atengk.iterator.ProductIterator;
import io.github.atengk.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 迭代器模式示例演示类
 */
@Component
public class IteratorPatternDemo {

    private final ProductCollectionImpl productCollection;

    public IteratorPatternDemo(ProductCollectionImpl productCollection) {
        this.productCollection = productCollection;
    }

    @PostConstruct
    public void runDemo() {
        System.out.println("=== 迭代器模式（Iterator Pattern）示例 ===");

        // 添加商品
        productCollection.addProduct(new Product("笔记本电脑", 5999.0));
        productCollection.addProduct(new Product("智能手机", 3999.0));
        productCollection.addProduct(new Product("蓝牙耳机", 499.0));

        // 创建迭代器
        ProductIterator iterator = productCollection.createIterator();

        // 遍历商品
        while (iterator.hasNext()) {
            Product product = iterator.next();
            System.out.println("商品名称：" + product.getName() + "，价格：" + product.getPrice());
        }
    }
}
