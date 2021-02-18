package com.shf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author songhaifeng
 */
@Slf4j
public class Sample {

    public static void main(String[] args) {
        // 支持三个list类型比较算法SIMPLE、LEVENSHTEIN_DISTANCE、AS_SET
        // 1、通常建议使用LEVENSHTEIN_DISTANCE，其相对比较智能，但对于超过300个元素的大集合，性能会比较差；
        // 2、SIMPLE相对性能最佳，属于线性比对，故导致了输出的结果非常的冗长；LEVENSHTEIN_DISTANCE相较于SIMPLE，LEVENSHTEIN_DISTANCE不关心元素移动的索引变化，更加精简
        // 具体查看https://javers.org/documentation/diff-configuration/#simple-vs-levenshtein
        // 3、AS_SET:在进行比对前优先将list转换为set，提高比对性能，其输出结果最精简，仅包含元素的添加和删除；
        Javers javers = JaversBuilder.javers()
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .build();

        Employee employee = Employee.builder().name("foo")
                .age(40)
                .salary(10_000)
                .primaryAddress(new Address("a", "b"))
                .skills(new HashSet<>(Arrays.asList("1", "2")))
                .skillsIgnore(new HashSet<>(Arrays.asList("1", "2")))
                .build();

        Employee employeeNew = Employee.builder().name("bar")
                .age(41)
                .salary(10_000)
                .primaryAddress(new Address("c", "d"))
                .skills(new HashSet<>(Arrays.asList("2", "1", "3")))
                .skillsIgnore(new HashSet<>(Arrays.asList("2", "1", "3")))
                .build();

        Diff diff = javers.compare(employee, employeeNew);
        log.info("------------------Diff------------------");
        log.info("Whether has changes : {}", diff.hasChanges());

        List<String> changedPropertyNames = new ArrayList<>();

        Changes changes = diff.getChanges();
        for (Change change : changes) {
            if (change instanceof PropertyChange) {
                changedPropertyNames.add(((PropertyChange) change).getPropertyNameWithPath());
            }
        }

        if (CollectionUtils.isNotEmpty(changedPropertyNames)) {
            log.info("Changed property names : {}", String.join(",", changedPropertyNames));
        }
        log.info("Show changed summary : {} ", diff.changesSummary());
        log.info("Show changed detail with pretty json :\n {}", javers.getJsonConverter().toJson(diff));
        log.info("Get the special property change info : {}", diff.getPropertyChanges("city"));


        log.info("------------------Same------------------");
        Employee employeeSame = Employee.builder().name("foo")
                .age(40)
                .salary(10_000)
                .primaryAddress(new Address("a", "b"))
                .skills(new HashSet<>(Arrays.asList("1", "2")))
                .skillsIgnore(new HashSet<>(Arrays.asList("4", "2")))
                .build();

        diff = javers.compare(employee, employeeSame);
        log.info("Whether has changes : {}", diff.hasChanges());

        log.info("------------------Compare with null------------------");
        diff = javers.compare(null, employeeSame);
        log.info("Whether has changes : {}", diff.hasChanges());
        for (Change change : diff.getChanges()) {
            if (change instanceof PropertyChange) {
                changedPropertyNames.add(((PropertyChange) change).getPropertyNameWithPath());
            }
        }
        if (CollectionUtils.isNotEmpty(changedPropertyNames)) {
            log.info("Changed property names : {}", String.join(",", changedPropertyNames));
        }
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class Employee {
        private String name;

        private int salary;

        private int age;

        private List<Employee> subordinates = new ArrayList<>();

        private Address primaryAddress;

        private Set<String> skills;

        @DiffIgnore
        private Set<String> skillsIgnore;
    }

    @Data
    @NoArgsConstructor
    static class Address {
        private String city;

        private String street;

        public Address(String city, String street) {
            this.city = city;
            this.street = street;
        }
    }
}
