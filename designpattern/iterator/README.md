## 🔁 迭代器模式（Iterator Pattern）

### 一、模式简介

**迭代器模式（Iterator Pattern）** 是一种行为型设计模式，用于提供一种**顺序访问聚合对象（集合、列表、容器）中元素**的方法，而无需暴露其内部结构。

📘 **核心思想：**

> 将遍历集合的行为从集合对象中分离出来，通过独立的迭代器对象来实现，使得集合结构与遍历逻辑解耦。

在实际项目中常用于：

* 封装复杂集合的访问方式（如多层目录遍历）
* 自定义对象集合的遍历规则（如分页、自定义顺序）
* 提供统一的遍历接口（对外只暴露 Iterator）

---

### 二、模式结构说明

```
Iterator（迭代器接口）
 ├── hasNext()：是否存在下一个元素
 ├── next()：返回下一个元素

ConcreteIterator（具体迭代器）
 ├── 保存当前遍历位置
 ├── 实现迭代逻辑

Aggregate（聚合接口）
 ├── 创建迭代器对象

ConcreteAggregate（具体集合）
 ├── 持有对象集合，返回相应迭代器
```

---

### 三、实战案例：自定义商品集合遍历器

**场景说明：**
在电商系统中，我们维护了一个自定义的商品集合 `ProductCollection`，
通过迭代器模式实现：

* 外部无需了解集合的内部实现（List、Set 都无关）
* 支持顺序遍历所有商品对象

---

### 四、代码结构

```
io.github.atengk
└── service
    └── iterator
        ├── iterator
        │    ├── ProductIterator.java
        │    └── ProductIteratorImpl.java
        ├── aggregate
        │    ├── ProductCollection.java
        │    └── ProductCollectionImpl.java
        ├── model
        │    └── Product.java
        └── IteratorPatternDemo.java
```

---

### 五、代码实现

#### 1️⃣ 商品实体类（Product）

```java
package io.github.atengk.service.iterator.model;

/**
 * 商品实体类
 */
public class Product {

    private final String name;
    private final double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
```

---

#### 2️⃣ 迭代器接口（Iterator）

```java
package io.github.atengk.service.iterator.iterator;

import io.github.atengk.service.iterator.model.Product;

/**
 * 商品迭代器接口
 */
public interface ProductIterator {

    /**
     * 判断是否还有下一个商品
     */
    boolean hasNext();

    /**
     * 获取下一个商品
     */
    Product next();
}
```

---

#### 3️⃣ 聚合接口（Aggregate）

```java
package io.github.atengk.service.iterator.aggregate;

import io.github.atengk.service.iterator.iterator.ProductIterator;

/**
 * 商品集合接口
 */
public interface ProductCollection {

    /**
     * 创建并返回一个商品迭代器
     */
    ProductIterator createIterator();
}
```

---

#### 4️⃣ 具体集合实现类（Concrete Aggregate）

```java
package io.github.atengk.service.iterator.aggregate;

import io.github.atengk.service.iterator.iterator.ProductIterator;
import io.github.atengk.service.iterator.iterator.ProductIteratorImpl;
import io.github.atengk.service.iterator.model.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品集合实现类
 */
@Component
public class ProductCollectionImpl implements ProductCollection {

    private final List<Product> productList = new ArrayList<>();

    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        productList.add(product);
    }

    /**
     * 获取所有商品
     */
    public List<Product> getAllProducts() {
        return productList;
    }

    /**
     * 创建一个新的商品迭代器
     */
    @Override
    public ProductIterator createIterator() {
        return new ProductIteratorImpl(productList);
    }
}
```

---

#### 5️⃣ 具体迭代器实现类（Concrete Iterator）

```java
package io.github.atengk.service.iterator.iterator;

import io.github.atengk.service.iterator.model.Product;

import java.util.List;

/**
 * 商品迭代器实现类
 */
public class ProductIteratorImpl implements ProductIterator {

    private final List<Product> productList;
    private int position = 0;

    public ProductIteratorImpl(List<Product> productList) {
        this.productList = productList;
    }

    @Override
    public boolean hasNext() {
        return position < productList.size();
    }

    @Override
    public Product next() {
        if (!hasNext()) {
            throw new IllegalStateException("没有更多商品了");
        }
        return productList.get(position++);
    }
}
```

---

#### 6️⃣ 示例运行类（Demo）

```java
package io.github.atengk.service.iterator;

import io.github.atengk.service.iterator.aggregate.ProductCollectionImpl;
import io.github.atengk.service.iterator.iterator.ProductIterator;
import io.github.atengk.service.iterator.model.Product;
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
```

---

### 六、输出示例

```
=== 迭代器模式（Iterator Pattern）示例 ===
商品名称：笔记本电脑，价格：5999.0
商品名称：智能手机，价格：3999.0
商品名称：蓝牙耳机，价格：499.0
```

---

### 七、总结与应用场景

✅ **优点：**

* 封装集合的内部实现，降低耦合。
* 提供统一遍历接口，支持多种集合实现。
* 可灵活扩展不同的遍历逻辑（顺序、逆序、过滤等）。

⚠️ **缺点：**

* 对简单集合结构来说，额外的迭代器类略显繁琐。

📌 **常见应用场景：**

* 需要统一遍历不同类型集合的场景（如文件列表、菜单树）。
* ORM 框架内部游标封装（如 MyBatis 的 ResultSetHandler）。
* 自定义分页 / 数据流式处理。

