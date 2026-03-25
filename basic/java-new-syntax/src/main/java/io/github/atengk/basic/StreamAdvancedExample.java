package io.github.atengk.basic;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stream 高级用法示例（分组、收集、聚合）
 */
public class StreamAdvancedExample {

    /**
     * 员工实体类（模拟项目中的业务对象）
     */
    static class Employee {
        private String name;
        private String dept;
        private int salary;

        public Employee(String name, String dept, int salary) {
            this.name = name;
            this.dept = dept;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public String getDept() {
            return dept;
        }

        public int getSalary() {
            return salary;
        }

        @Override
        public String toString() {
            return name + "(" + salary + ")";
        }
    }

    /**
     * 核心方法：演示分组、收集、聚合操作
     */
    public static void streamAdvancedUsage(List<Employee> list) {

        // =========================
        // 1. 分组（groupingBy）
        // =========================
        // 按部门分组
        Map<String, List<Employee>> groupByDept =
                list.stream()
                        .collect(Collectors.groupingBy(Employee::getDept));

        System.out.println("按部门分组：");
        groupByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 2. 分组 + 聚合（统计人数）
        // =========================
        Map<String, Long> countByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.counting()
                        ));

        System.out.println("\n各部门人数：");
        countByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 3. 分组 + 求平均值
        // =========================
        Map<String, Double> avgSalaryByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.averagingInt(Employee::getSalary)
                        ));

        System.out.println("\n各部门平均薪资：");
        avgSalaryByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 4. 分组 + 最大值
        // =========================
        Map<String, Optional<Employee>> maxSalaryByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.maxBy(Comparator.comparingInt(Employee::getSalary))
                        ));

        System.out.println("\n各部门最高薪资员工：");
        maxSalaryByDept.forEach((k, v) -> System.out.println(k + " -> " + v.orElse(null)));


        // =========================
        // 5. 分组 + 转换（只要姓名）
        // =========================
        Map<String, List<String>> namesByDept =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.mapping(Employee::getName, Collectors.toList())
                        ));

        System.out.println("\n各部门员工姓名：");
        namesByDept.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 6. 汇总（总薪资）
        // =========================
        int totalSalary =
                list.stream()
                        .mapToInt(Employee::getSalary)
                        .sum();

        System.out.println("\n总薪资：" + totalSalary);


        // =========================
        // 7. 多级分组（部门 + 薪资等级）
        // =========================
        Map<String, Map<String, List<Employee>>> multiGroup =
                list.stream()
                        .collect(Collectors.groupingBy(
                                Employee::getDept,
                                Collectors.groupingBy(e -> e.getSalary() > 10000 ? "高薪" : "普通")
                        ));

        System.out.println("\n多级分组：");
        multiGroup.forEach((dept, map) -> {
            System.out.println(dept + " -> " + map);
        });


        // =========================
        // 8. 转 Map（key 冲突处理）
        // =========================
        Map<String, Employee> nameMap =
                list.stream()
                        .collect(Collectors.toMap(
                                Employee::getName,
                                e -> e,
                                (oldVal, newVal) -> newVal // key冲突时取后者
                        ));

        System.out.println("\n转 Map：");
        nameMap.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 9. 分区（partitioningBy）
        // =========================
        Map<Boolean, List<Employee>> partition =
                list.stream()
                        .collect(Collectors.partitioningBy(e -> e.getSalary() > 10000));

        System.out.println("\n高薪/非高薪分区：");
        partition.forEach((k, v) -> System.out.println(k + " -> " + v));


        // =========================
        // 10. 收集为不可变集合（JDK16+）
        // =========================
        List<Employee> immutableList =
                list.stream().toList(); // 不可变集合

        System.out.println("\n不可变集合：" + immutableList);
    }


    public static void main(String[] args) {

        // 构造测试数据
        List<Employee> list = List.of(
                new Employee("张三", "IT", 12000),
                new Employee("李四", "IT", 9000),
                new Employee("王五", "HR", 8000),
                new Employee("赵六", "HR", 15000),
                new Employee("孙七", "Finance", 20000)
        );

        // 调用核心方法
        streamAdvancedUsage(list);
    }
}