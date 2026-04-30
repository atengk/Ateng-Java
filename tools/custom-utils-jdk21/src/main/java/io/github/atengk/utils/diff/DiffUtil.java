package io.github.atengk.utils.diff;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 差异对比工具类，覆盖单值、对象、集合、Map、文本、JSON、文件、补丁、审计日志、树、快照和同步数据等常见项目场景。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class DiffUtil {
    private static final String ROOT = "$";
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private DiffUtil() {
        throw new UnsupportedOperationException("DiffUtil 是静态工具类，不能实例化");
    }

    /**
     * 差异类型。
     */
    public enum DiffType {ADDED, REMOVED, MODIFIED, UNCHANGED}

    /**
     * 通用新旧值对。
     */
    public record Pair<T>(T oldValue, T newValue) {
    }

    /**
     * 通用差异项。
     */
    public record DiffItem(String fieldName, String label, String path, Object oldValue, Object newValue,
                           DiffType type) {
    }

    /**
     * 差异结果。
     */
    public record DiffResult(List<DiffItem> items) {
        /**
         * 创建差异结果。
         */
        public DiffResult {
            items = items == null ? List.of() : List.copyOf(items);
        }

        /**
         * 判断是否存在差异。
         */
        public boolean hasDiff() {
            return items.stream().anyMatch(i -> i.type != DiffType.UNCHANGED);
        }
    }

    /**
     * 字段差异。
     */
    public record FieldDiff(String fieldName, String label, Object oldValue, Object newValue, DiffType type) {
    }

    /**
     * 集合差异。
     */
    public record CollectionDiff<T>(List<T> added, List<T> removed, List<T> unchanged, List<Pair<T>> modified) {
        /**
         * 创建集合差异。
         */
        public CollectionDiff {
            added = added == null ? List.of() : List.copyOf(added);
            removed = removed == null ? List.of() : List.copyOf(removed);
            unchanged = unchanged == null ? List.of() : List.copyOf(unchanged);
            modified = modified == null ? List.of() : List.copyOf(modified);
        }
    }

    /**
     * Map 差异。
     */
    public record MapDiff<K, V>(Map<K, V> addedEntries, Map<K, V> removedEntries, Map<K, Pair<V>> changedEntries,
                                Map<K, V> unchangedEntries) {
        /**
         * 创建 Map 差异。
         */
        public MapDiff {
            addedEntries = addedEntries == null ? Map.of() : Map.copyOf(addedEntries);
            removedEntries = removedEntries == null ? Map.of() : Map.copyOf(removedEntries);
            changedEntries = changedEntries == null ? Map.of() : Map.copyOf(changedEntries);
            unchangedEntries = unchangedEntries == null ? Map.of() : Map.copyOf(unchangedEntries);
        }
    }

    /**
     * 文本差异。
     */
    public record TextDiff(List<DiffItem> items) {
        /**
         * 创建文本差异。
         */
        public TextDiff {
            items = items == null ? List.of() : List.copyOf(items);
        }

        /**
         * 判断是否存在文本差异。
         */
        public boolean hasDiff() {
            return items.stream().anyMatch(i -> i.type != DiffType.UNCHANGED);
        }
    }

    /**
     * 补丁条目。
     */
    public record PatchEntry(DiffType type, String value) {
    }

    /**
     * 文本补丁。
     */
    public record DiffPatch(String oldText, String newText, List<PatchEntry> entries) {
        /**
         * 创建文本补丁。
         */
        public DiffPatch {
            oldText = oldText == null ? "" : oldText;
            newText = newText == null ? "" : newText;
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        /**
         * 转为补丁文本。
         */
        public String toText() {
            return entries.stream().map(e -> (e.type == DiffType.ADDED ? "+ " : e.type == DiffType.REMOVED ? "- " : e.type == DiffType.MODIFIED ? "~ " : "  ") + e.value).collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * 树差异。
     */
    public record TreeDiff<T>(List<T> addedNodes, List<T> removedNodes, List<Pair<T>> modifiedNodes,
                              List<T> movedNodes) {
        /**
         * 创建树差异。
         */
        public TreeDiff {
            addedNodes = addedNodes == null ? List.of() : List.copyOf(addedNodes);
            removedNodes = removedNodes == null ? List.of() : List.copyOf(removedNodes);
            modifiedNodes = modifiedNodes == null ? List.of() : List.copyOf(modifiedNodes);
            movedNodes = movedNodes == null ? List.of() : List.copyOf(movedNodes);
        }
    }

    /**
     * 变更日志。
     */
    public record ChangeLog(String fieldName, String label, String content, DiffType type) {
    }

    /**
     * 差异摘要。
     */
    public record DiffSummary(int addedCount, int removedCount, int modifiedCount, int unchangedCount, int totalCount) {
    }

    /**
     * 差异选项。
     */
    public record DiffOptions(Set<String> includeFields, Set<String> ignoreFields, Set<String> ignorePaths,
                              Map<String, String> fieldLabels,
                              Map<String, BiPredicate<Object, Object>> fieldComparators,
                              boolean ignoreNull, boolean ignoreNewNull, boolean ignoreOldNull, boolean ignoreBlank,
                              boolean ignoreCase, boolean ignoreOrder, boolean deepCompare, boolean ignoreTransient,
                              boolean ignoreStatic, ChronoUnit ignoreTimePrecision, boolean ignoreNumberScale,
                              int maxDepth) {
        /**
         * 创建差异选项。
         */
        public DiffOptions {
            includeFields = includeFields == null ? Set.of() : Set.copyOf(includeFields);
            ignoreFields = ignoreFields == null ? Set.of() : Set.copyOf(ignoreFields);
            ignorePaths = ignorePaths == null ? Set.of() : Set.copyOf(ignorePaths);
            fieldLabels = fieldLabels == null ? Map.of() : Map.copyOf(fieldLabels);
            fieldComparators = fieldComparators == null ? Map.of() : Map.copyOf(fieldComparators);
            maxDepth = Math.max(1, maxDepth);
        }

        /**
         * 获取默认选项。
         */
        public static DiffOptions defaults() {
            return builder().build();
        }

        /**
         * 创建构建器。
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 差异选项构建器。
         */
        public static final class Builder {
            private Set<String> includeFields = new LinkedHashSet<>(), ignoreFields = new LinkedHashSet<>(), ignorePaths = new LinkedHashSet<>();
            private Map<String, String> fieldLabels = new LinkedHashMap<>();
            private Map<String, BiPredicate<Object, Object>> fieldComparators = new LinkedHashMap<>();
            private boolean ignoreNull, ignoreNewNull, ignoreOldNull, ignoreBlank, ignoreCase, ignoreOrder, deepCompare, ignoreNumberScale;
            private boolean ignoreTransient = true, ignoreStatic = true;
            private ChronoUnit ignoreTimePrecision;
            private int maxDepth = 16;

            private Builder() {
            }

            /**
             * 设置包含字段。
             */
            public Builder includeFields(Collection<String> v) {
                includeFields = strSet(v);
                return this;
            }

            /**
             * 设置忽略字段。
             */
            public Builder ignoreFields(Collection<String> v) {
                ignoreFields = strSet(v);
                return this;
            }

            /**
             * 设置忽略路径。
             */
            public Builder ignorePaths(Collection<String> v) {
                ignorePaths = strSet(v);
                return this;
            }

            /**
             * 设置字段展示名。
             */
            public Builder fieldLabels(Map<String, String> v) {
                fieldLabels = v == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v);
                return this;
            }

            /**
             * 设置字段比较器。
             */
            public Builder fieldComparators(Map<String, BiPredicate<Object, Object>> v) {
                fieldComparators = v == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v);
                return this;
            }

            /**
             * 添加字段比较器。
             */
            public Builder fieldComparator(String n, BiPredicate<Object, Object> c) {
                if (n != null && c != null) fieldComparators.put(n, c);
                return this;
            }

            /**
             * 设置忽略 null。
             */
            public Builder ignoreNull(boolean v) {
                ignoreNull = v;
                return this;
            }

            /**
             * 设置忽略新值 null。
             */
            public Builder ignoreNewNull(boolean v) {
                ignoreNewNull = v;
                return this;
            }

            /**
             * 设置忽略旧值 null。
             */
            public Builder ignoreOldNull(boolean v) {
                ignoreOldNull = v;
                return this;
            }

            /**
             * 设置忽略空白字符串。
             */
            public Builder ignoreBlank(boolean v) {
                ignoreBlank = v;
                return this;
            }

            /**
             * 设置忽略大小写。
             */
            public Builder ignoreCase(boolean v) {
                ignoreCase = v;
                return this;
            }

            /**
             * 设置忽略顺序。
             */
            public Builder ignoreOrder(boolean v) {
                ignoreOrder = v;
                return this;
            }

            /**
             * 设置深度比较。
             */
            public Builder deepCompare(boolean v) {
                deepCompare = v;
                return this;
            }

            /**
             * 设置忽略 transient。
             */
            public Builder ignoreTransient(boolean v) {
                ignoreTransient = v;
                return this;
            }

            /**
             * 设置忽略 static。
             */
            public Builder ignoreStatic(boolean v) {
                ignoreStatic = v;
                return this;
            }

            /**
             * 设置忽略时间精度。
             */
            public Builder ignoreTimePrecision(ChronoUnit v) {
                ignoreTimePrecision = v;
                return this;
            }

            /**
             * 设置忽略数字精度格式。
             */
            public Builder ignoreNumberScale(boolean v) {
                ignoreNumberScale = v;
                return this;
            }

            /**
             * 设置最大深度。
             */
            public Builder maxDepth(int v) {
                maxDepth = v;
                return this;
            }

            /**
             * 构建选项。
             */
            public DiffOptions build() {
                return new DiffOptions(includeFields, ignoreFields, ignorePaths, fieldLabels, fieldComparators, ignoreNull, ignoreNewNull, ignoreOldNull, ignoreBlank, ignoreCase, ignoreOrder, deepCompare, ignoreTransient, ignoreStatic, ignoreTimePrecision, ignoreNumberScale, maxDepth);
            }
        }
    }

    /**
     * 判断两个值是否一致。
     */
    public static boolean isSame(Object oldValue, Object newValue) {
        return Objects.equals(oldValue, newValue);
    }

    /**
     * 判断两个值是否不同。
     */
    public static boolean isDifferent(Object oldValue, Object newValue) {
        return !isSame(oldValue, newValue);
    }

    /**
     * 判断是否存在差异。
     */
    public static boolean hasDiff(Object oldValue, Object newValue) {
        return isDifferent(oldValue, newValue);
    }

    /**
     * 判断是否无差异。
     */
    public static boolean hasNoDiff(Object oldValue, Object newValue) {
        return isSame(oldValue, newValue);
    }

    /**
     * 判断是否变化。
     */
    public static boolean isChanged(Object oldValue, Object newValue) {
        return isDifferent(oldValue, newValue);
    }

    /**
     * 判断是否未变化。
     */
    public static boolean isUnchanged(Object oldValue, Object newValue) {
        return isSame(oldValue, newValue);
    }

    /**
     * 不同时返回新值，相同时返回旧值。
     */
    public static <T> T firstChanged(T oldValue, T newValue) {
        return isDifferent(oldValue, newValue) ? newValue : oldValue;
    }

    /**
     * 相同时返回默认值，否则返回新值。
     */
    public static <T> T defaultIfSame(T oldValue, T newValue, T defaultValue) {
        return isSame(oldValue, newValue) ? defaultValue : newValue;
    }

    /**
     * 不同时返回默认值，否则返回旧值。
     */
    public static <T> T defaultIfDifferent(T oldValue, T newValue, T defaultValue) {
        return isDifferent(oldValue, newValue) ? defaultValue : oldValue;
    }

    /**
     * 对比单个字段值。
     */
    public static DiffItem diffValue(String fieldName, Object oldValue, Object newValue) {
        return diffValue(fieldName, oldValue, newValue, fieldName);
    }

    /**
     * 对比单个字段值并指定展示名称。
     */
    public static DiffItem diffValue(String fieldName, Object oldValue, Object newValue, String label) {
        String n = text(fieldName, "fieldName");
        return new DiffItem(n, label == null ? n : label, n, oldValue, newValue, type(oldValue, newValue, Objects.equals(oldValue, newValue)));
    }

    /**
     * 仅变化时返回差异项。
     */
    public static Optional<DiffItem> diffIfChanged(String fieldName, Object oldValue, Object newValue) {
        DiffItem i = diffValue(fieldName, oldValue, newValue);
        return i.type == DiffType.UNCHANGED ? Optional.empty() : Optional.of(i);
    }

    /**
     * 支持 null 的字段对比。
     */
    public static DiffItem diffNullable(String fieldName, Object oldValue, Object newValue) {
        return diffValue(fieldName, oldValue, newValue);
    }

    /**
     * 对比枚举值。
     */
    public static DiffItem diffEnum(String fieldName, Enum<?> oldValue, Enum<?> newValue) {
        return diffValue(fieldName, oldValue == null ? null : oldValue.name(), newValue == null ? null : newValue.name());
    }

    /**
     * 对比布尔值。
     */
    public static DiffItem diffBoolean(String fieldName, Boolean oldValue, Boolean newValue) {
        return diffValue(fieldName, oldValue, newValue);
    }

    /**
     * 对比数字值。
     */
    public static DiffItem diffNumber(String fieldName, Number oldValue, Number newValue) {
        return diffValue(fieldName, oldValue, newValue);
    }

    /**
     * 对比 BigDecimal 值。
     */
    public static DiffItem diffBigDecimal(String fieldName, BigDecimal oldValue, BigDecimal newValue) {
        return diffValue(fieldName, oldValue, newValue);
    }

    /**
     * 按小数位对比 BigDecimal 值。
     */
    public static DiffItem diffBigDecimal(String fieldName, BigDecimal oldValue, BigDecimal newValue, int scale) {
        if (scale < 0) throw new IllegalArgumentException("scale 不能小于 0");
        return diffValue(fieldName, oldValue == null ? null : oldValue.setScale(scale, RoundingMode.HALF_UP), newValue == null ? null : newValue.setScale(scale, RoundingMode.HALF_UP));
    }

    /**
     * 对比时间值。
     */
    public static DiffItem diffDateTime(String fieldName, Temporal oldValue, Temporal newValue) {
        return diffValue(fieldName, oldValue, newValue);
    }

    /**
     * 使用自定义比较器对比字段值。
     */
    public static <T> DiffItem diffWithComparator(String fieldName, T oldValue, T newValue, BiPredicate<T, T> samePredicate) {
        Objects.requireNonNull(samePredicate, "samePredicate 不能为 null");
        boolean same = samePredicate.test(oldValue, newValue);
        return new DiffItem(text(fieldName, "fieldName"), fieldName, fieldName, oldValue, newValue, type(oldValue, newValue, same));
    }

    /**
     * 使用容差对比数字值。
     */
    public static DiffItem diffNumberWithTolerance(String fieldName, BigDecimal oldValue, BigDecimal newValue, BigDecimal tolerance) {
        boolean diff = isDifferentWithTolerance(oldValue, newValue, tolerance);
        return new DiffItem(text(fieldName, "fieldName"), fieldName, fieldName, oldValue, newValue, type(oldValue, newValue, !diff));
    }

    /**
     * 判断两个数字值是否超过容差。
     */
    public static boolean isDifferentWithTolerance(BigDecimal oldValue, BigDecimal newValue, BigDecimal tolerance) {
        Objects.requireNonNull(tolerance, "tolerance 不能为 null");
        if (tolerance.signum() < 0) throw new IllegalArgumentException("tolerance 不能小于 0");
        if (oldValue == null || newValue == null) return !Objects.equals(oldValue, newValue);
        return oldValue.subtract(newValue).abs().compareTo(tolerance) > 0;
    }

    /**
     * 对比两个 Java Bean 的字段差异。
     */
    public static DiffResult diffBean(Object oldBean, Object newBean) {
        return diffBean(oldBean, newBean, DiffOptions.defaults());
    }

    /**
     * 按选项对比两个 Java Bean 的字段差异。
     */
    public static DiffResult diffBean(Object oldBean, Object newBean, DiffOptions options) {
        DiffOptions opt = options == null ? DiffOptions.defaults() : options;
        if (oldBean == null && newBean == null) return new DiffResult(List.of());
        if (oldBean == null || newBean == null)
            return new DiffResult(List.of(new DiffItem(null, null, ROOT, oldBean, newBean, oldBean == null ? DiffType.ADDED : DiffType.REMOVED)));
        sameClass(oldBean, newBean);
        List<DiffItem> out = new ArrayList<>();
        for (String f : fieldNames(oldBean.getClass(), opt)) {
            if (!compareField(f, f, opt)) continue;
            Object ov = fieldValue(oldBean, f), nv = fieldValue(newBean, f);
            if (opt.deepCompare && !simple(ov) && !simple(nv)) deep(ov, nv, f, out, opt, 1, new IdentityHashMap<>());
            else if (!sameByOption(f, f, ov, nv, opt))
                out.add(new DiffItem(f, opt.fieldLabels.getOrDefault(f, f), f, ov, nv, type(ov, nv, false)));
        }
        return new DiffResult(out);
    }

    /**
     * 返回两个对象的全部直接字段对比结果。
     */
    public static List<FieldDiff> diffFields(Object oldBean, Object newBean) {
        requireBeans(oldBean, newBean);
        List<FieldDiff> r = new ArrayList<>();
        for (String f : fieldNames(oldBean.getClass(), DiffOptions.defaults())) {
            Object ov = fieldValue(oldBean, f), nv = fieldValue(newBean, f);
            r.add(new FieldDiff(f, f, ov, nv, type(ov, nv, Objects.equals(ov, nv))));
        }
        return List.copyOf(r);
    }

    /**
     * 对比指定字段。
     */
    public static FieldDiff diffField(Object oldBean, Object newBean, String fieldName) {
        requireBeans(oldBean, newBean);
        String f = text(fieldName, "fieldName");
        Object ov = fieldValue(oldBean, f), nv = fieldValue(newBean, f);
        return new FieldDiff(f, f, ov, nv, type(ov, nv, Objects.equals(ov, nv)));
    }

    /**
     * 只比较指定字段。
     */
    public static DiffResult diffIncludeFields(Object oldBean, Object newBean, Collection<String> fields) {
        return diffBean(oldBean, newBean, DiffOptions.builder().includeFields(fields).build());
    }

    /**
     * 排除指定字段后比较。
     */
    public static DiffResult diffExcludeFields(Object oldBean, Object newBean, Collection<String> fields) {
        return diffBean(oldBean, newBean, DiffOptions.builder().ignoreFields(fields).build());
    }

    /**
     * 返回对象已变化字段。
     */
    public static DiffResult diffChangedFields(Object oldBean, Object newBean) {
        return diffBean(oldBean, newBean);
    }

    /**
     * 返回对象未变化字段。
     */
    public static List<FieldDiff> diffUnchangedFields(Object oldBean, Object newBean) {
        return diffFields(oldBean, newBean).stream().filter(d -> d.type == DiffType.UNCHANGED).toList();
    }

    /**
     * 获取发生变化的字段名。
     */
    public static List<String> getChangedFieldNames(Object oldBean, Object newBean) {
        return diffBean(oldBean, newBean).items.stream().map(DiffItem::fieldName).filter(Objects::nonNull).toList();
    }

    /**
     * 判断指定字段是否变化。
     */
    public static boolean hasFieldChanged(Object oldBean, Object newBean, String fieldName) {
        return diffField(oldBean, newBean, fieldName).type != DiffType.UNCHANGED;
    }

    /**
     * 判断任一字段是否变化。
     */
    public static boolean hasAnyFieldChanged(Object oldBean, Object newBean, Collection<String> fields) {
        return fields != null && fields.stream().anyMatch(f -> hasFieldChanged(oldBean, newBean, f));
    }

    /**
     * 判断全部字段是否变化。
     */
    public static boolean hasAllFieldsChanged(Object oldBean, Object newBean, Collection<String> fields) {
        return fields != null && !fields.isEmpty() && fields.stream().allMatch(f -> hasFieldChanged(oldBean, newBean, f));
    }

    /**
     * 深度对比两个对象。
     */
    public static DiffResult diffDeep(Object oldObject, Object newObject) {
        return diffDeep(oldObject, newObject, DiffOptions.builder().deepCompare(true).build());
    }

    /**
     * 按选项深度对比两个对象。
     */
    public static DiffResult diffDeep(Object oldObject, Object newObject, DiffOptions options) {
        List<DiffItem> out = new ArrayList<>();
        deep(oldObject, newObject, ROOT, out, options == null ? DiffOptions.defaults() : options, 0, new IdentityHashMap<>());
        return new DiffResult(out);
    }

    /**
     * 对比指定对象路径。
     */
    public static DiffResult diffByPath(Object oldObject, Object newObject, String path) {
        String p = text(path, "path");
        DiffItem i = diffValue(p, readPath(oldObject, p), readPath(newObject, p));
        return i.type == DiffType.UNCHANGED ? new DiffResult(List.of()) : new DiffResult(List.of(i));
    }

    /**
     * 获取深度对比变化路径。
     */
    public static List<String> getChangedPaths(Object oldObject, Object newObject) {
        return diffDeep(oldObject, newObject).items.stream().map(DiffItem::path).toList();
    }

    /**
     * 判断指定路径是否变化。
     */
    public static boolean hasPathChanged(Object oldObject, Object newObject, String path) {
        return diffByPath(oldObject, newObject, path).hasDiff();
    }

    /**
     * 扁平化对象差异。
     */
    public static DiffResult flattenDiff(Object oldObject, Object newObject) {
        return diffDeep(oldObject, newObject);
    }

    /**
     * 对比嵌套对象并指定父路径。
     */
    public static DiffResult diffNestedBean(Object oldBean, Object newBean, String parentPath) {
        List<DiffItem> out = new ArrayList<>();
        deep(oldBean, newBean, text(parentPath, "parentPath"), out, DiffOptions.builder().deepCompare(true).build(), 0, new IdentityHashMap<>());
        return new DiffResult(out);
    }

    /**
     * 对比两个集合。
     */
    public static <T> CollectionDiff<T> diffCollection(Collection<T> oldCollection, Collection<T> newCollection) {
        return new CollectionDiff<>(getAdded(oldCollection, newCollection), getRemoved(oldCollection, newCollection), getRetained(oldCollection, newCollection), List.of());
    }

    /**
     * 对比两个列表。
     */
    public static <T> CollectionDiff<T> diffList(List<T> oldList, List<T> newList) {
        return diffCollection(oldList, newList);
    }

    /**
     * 对比两个 Set。
     */
    public static <T> CollectionDiff<T> diffSet(Set<T> oldSet, Set<T> newSet) {
        return diffCollection(oldSet, newSet);
    }

    /**
     * 对比两个数组。
     */
    public static <T> CollectionDiff<T> diffArray(T[] oldArray, T[] newArray) {
        return diffCollection(oldArray == null ? List.of() : Arrays.asList(oldArray), newArray == null ? List.of() : Arrays.asList(newArray));
    }

    /**
     * 获取新增元素。
     */
    public static <T> List<T> getAdded(Collection<T> oldCollection, Collection<T> newCollection) {
        List<T> old = new ArrayList<>(safe(oldCollection));
        List<T> r = new ArrayList<>();
        for (T n : safe(newCollection)) if (!old.remove(n)) r.add(n);
        return List.copyOf(r);
    }

    /**
     * 获取删除元素。
     */
    public static <T> List<T> getRemoved(Collection<T> oldCollection, Collection<T> newCollection) {
        return getAdded(newCollection, oldCollection);
    }

    /**
     * 获取交集元素。
     */
    public static <T> List<T> getRetained(Collection<T> oldCollection, Collection<T> newCollection) {
        List<T> neu = new ArrayList<>(safe(newCollection));
        List<T> r = new ArrayList<>();
        for (T o : safe(oldCollection)) if (neu.remove(o)) r.add(o);
        return List.copyOf(r);
    }

    /**
     * 获取未变化元素。
     */
    public static <T> List<T> getUnchanged(Collection<T> oldCollection, Collection<T> newCollection) {
        return getRetained(oldCollection, newCollection);
    }

    /**
     * 获取变化元素。
     */
    public static <T> List<T> getChanged(Collection<T> oldCollection, Collection<T> newCollection) {
        List<T> r = new ArrayList<>(getRemoved(oldCollection, newCollection));
        r.addAll(getAdded(oldCollection, newCollection));
        return List.copyOf(r);
    }

    /**
     * 判断是否存在新增。
     */
    public static boolean hasAdded(Collection<?> oldCollection, Collection<?> newCollection) {
        return !addedRaw(oldCollection, newCollection).isEmpty();
    }

    /**
     * 判断是否存在删除。
     */
    public static boolean hasRemoved(Collection<?> oldCollection, Collection<?> newCollection) {
        return !addedRaw(newCollection, oldCollection).isEmpty();
    }

    /**
     * 判断集合是否变化。
     */
    public static boolean hasCollectionChanged(Collection<?> oldCollection, Collection<?> newCollection) {
        return hasAdded(oldCollection, newCollection) || hasRemoved(oldCollection, newCollection);
    }

    /**
     * 按 key 对比列表。
     */
    public static <T, K> CollectionDiff<T> diffListByKey(List<T> oldList, List<T> newList, Function<T, K> keyExtractor) {
        return diffCollectionByKey(oldList, newList, keyExtractor);
    }

    /**
     * 按 key 对比集合。
     */
    public static <T, K> CollectionDiff<T> diffCollectionByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor) {
        return diffElementByKey(oldCollection, newCollection, keyExtractor, Objects::equals);
    }

    /**
     * 按 key 获取新增元素。
     */
    public static <T, K> List<T> getAddedByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor) {
        return diffCollectionByKey(oldCollection, newCollection, keyExtractor).added;
    }

    /**
     * 按 key 获取删除元素。
     */
    public static <T, K> List<T> getRemovedByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor) {
        return diffCollectionByKey(oldCollection, newCollection, keyExtractor).removed;
    }

    /**
     * 按 key 获取修改元素。
     */
    public static <T, K> List<Pair<T>> getModifiedByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor) {
        return diffCollectionByKey(oldCollection, newCollection, keyExtractor).modified;
    }

    /**
     * 按 key 和自定义规则对比元素。
     */
    public static <T, K> CollectionDiff<T> diffElementByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor, BiPredicate<T, T> samePredicate) {
        Objects.requireNonNull(keyExtractor, "keyExtractor 不能为 null");
        Objects.requireNonNull(samePredicate, "samePredicate 不能为 null");
        Map<K, T> old = keyMap(oldCollection, keyExtractor), neu = keyMap(newCollection, keyExtractor);
        List<T> add = new ArrayList<>(), rem = new ArrayList<>(), un = new ArrayList<>();
        List<Pair<T>> mod = new ArrayList<>();
        for (Map.Entry<K, T> e : neu.entrySet()) {
            if (!old.containsKey(e.getKey())) add.add(e.getValue());
            else if (samePredicate.test(old.get(e.getKey()), e.getValue())) un.add(e.getValue());
            else mod.add(new Pair<>(old.get(e.getKey()), e.getValue()));
        }
        for (Map.Entry<K, T> e : old.entrySet()) if (!neu.containsKey(e.getKey())) rem.add(e.getValue());
        return new CollectionDiff<>(add, rem, un, mod);
    }

    /**
     * 按 key 对比元素字段。
     */
    public static <T, K> DiffResult diffElementFieldsByKey(Collection<T> oldCollection, Collection<T> newCollection, Function<T, K> keyExtractor) {
        Map<K, T> old = keyMap(oldCollection, keyExtractor), neu = keyMap(newCollection, keyExtractor);
        List<DiffItem> out = new ArrayList<>();
        for (K k : neu.keySet())
            if (old.containsKey(k))
                diffBean(old.get(k), neu.get(k)).items.forEach(i -> out.add(new DiffItem(i.fieldName, i.label, "[" + k + "]." + i.path, i.oldValue, i.newValue, i.type)));
        return new DiffResult(out);
    }

    /**
     * 对比两个 Map。
     */
    public static <K, V> MapDiff<K, V> diffMap(Map<K, V> oldMap, Map<K, V> newMap) {
        return diffMap(oldMap, newMap, DiffOptions.defaults());
    }

    /**
     * 按选项对比两个 Map。
     */
    public static <K, V> MapDiff<K, V> diffMap(Map<K, V> oldMap, Map<K, V> newMap, DiffOptions options) {
        Map<K, V> old = safeMap(oldMap), neu = safeMap(newMap);
        DiffOptions opt = options == null ? DiffOptions.defaults() : options;
        Map<K, V> add = new LinkedHashMap<>(), rem = new LinkedHashMap<>(), un = new LinkedHashMap<>();
        Map<K, Pair<V>> mod = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : neu.entrySet()) {
            if (!old.containsKey(e.getKey())) add.put(e.getKey(), e.getValue());
            else if (sameByOption(String.valueOf(e.getKey()), String.valueOf(e.getKey()), old.get(e.getKey()), e.getValue(), opt))
                un.put(e.getKey(), e.getValue());
            else mod.put(e.getKey(), new Pair<>(old.get(e.getKey()), e.getValue()));
        }
        for (Map.Entry<K, V> e : old.entrySet()) if (!neu.containsKey(e.getKey())) rem.put(e.getKey(), e.getValue());
        return new MapDiff<>(add, rem, mod, un);
    }

    /**
     * 获取新增 key。
     */
    public static <K> Set<K> getAddedKeys(Map<K, ?> oldMap, Map<K, ?> newMap) {
        Set<K> r = new LinkedHashSet<>(safeMap(newMap).keySet());
        r.removeAll(safeMap(oldMap).keySet());
        return Set.copyOf(r);
    }

    /**
     * 获取删除 key。
     */
    public static <K> Set<K> getRemovedKeys(Map<K, ?> oldMap, Map<K, ?> newMap) {
        return getAddedKeys(newMap, oldMap);
    }

    /**
     * 获取值变化 key。
     */
    public static <K> Set<K> getChangedKeys(Map<K, ?> oldMap, Map<K, ?> newMap) {
        Set<K> r = new LinkedHashSet<>();
        Map<K, ?> old = safeMap(oldMap), neu = safeMap(newMap);
        for (K k : old.keySet()) if (neu.containsKey(k) && !Objects.equals(old.get(k), neu.get(k))) r.add(k);
        return Set.copyOf(r);
    }

    /**
     * 获取值未变化 key。
     */
    public static <K> Set<K> getUnchangedKeys(Map<K, ?> oldMap, Map<K, ?> newMap) {
        Set<K> r = new LinkedHashSet<>();
        Map<K, ?> old = safeMap(oldMap), neu = safeMap(newMap);
        for (K k : old.keySet()) if (neu.containsKey(k) && Objects.equals(old.get(k), neu.get(k))) r.add(k);
        return Set.copyOf(r);
    }

    /**
     * 获取新增键值。
     */
    public static <K, V> Map<K, V> getAddedEntries(Map<K, V> oldMap, Map<K, V> newMap) {
        return diffMap(oldMap, newMap).addedEntries;
    }

    /**
     * 获取删除键值。
     */
    public static <K, V> Map<K, V> getRemovedEntries(Map<K, V> oldMap, Map<K, V> newMap) {
        return diffMap(oldMap, newMap).removedEntries;
    }

    /**
     * 获取变化键值。
     */
    public static <K, V> Map<K, Pair<V>> getChangedEntries(Map<K, V> oldMap, Map<K, V> newMap) {
        return diffMap(oldMap, newMap).changedEntries;
    }

    /**
     * 判断指定 key 是否变化。
     */
    public static boolean hasKeyChanged(Map<?, ?> oldMap, Map<?, ?> newMap, Object key) {
        Map<?, ?> old = safeMap(oldMap), neu = safeMap(newMap);
        return old.containsKey(key) != neu.containsKey(key) || !Objects.equals(old.get(key), neu.get(key));
    }

    /**
     * 判断 Map 是否变化。
     */
    public static boolean hasMapChanged(Map<?, ?> oldMap, Map<?, ?> newMap) {
        return !addedKeysRaw(oldMap, newMap).isEmpty() || !addedKeysRaw(newMap, oldMap).isEmpty() || !changedKeysRaw(oldMap, newMap).isEmpty();
    }

    /**
     * 对比文本，默认按行。
     */
    public static TextDiff diffText(String oldText, String newText) {
        return diffLines(oldText, newText);
    }

    /**
     * 字符级文本差异。
     */
    public static TextDiff diffChars(String oldText, String newText) {
        return diffTokens(chars(oldText), chars(newText), "char");
    }

    /**
     * 单词级文本差异。
     */
    public static TextDiff diffWords(String oldText, String newText) {
        return diffTokens(words(oldText), words(newText), "word");
    }

    /**
     * 行级文本差异。
     */
    public static TextDiff diffLines(String oldText, String newText) {
        return diffTokens(lines(oldText), lines(newText), "line");
    }

    /**
     * 获取新增行。
     */
    public static List<String> getAddedLines(String oldText, String newText) {
        return diffLines(oldText, newText).items.stream().filter(i -> i.type == DiffType.ADDED).map(i -> String.valueOf(i.newValue)).toList();
    }

    /**
     * 获取删除行。
     */
    public static List<String> getRemovedLines(String oldText, String newText) {
        return diffLines(oldText, newText).items.stream().filter(i -> i.type == DiffType.REMOVED).map(i -> String.valueOf(i.oldValue)).toList();
    }

    /**
     * 获取变化行。
     */
    public static List<String> getChangedLines(String oldText, String newText) {
        return diffLines(oldText, newText).items.stream().filter(i -> i.type == DiffType.MODIFIED).map(i -> i.oldValue + " -> " + i.newValue).toList();
    }

    /**
     * 判断文本是否变化。
     */
    public static boolean hasTextChanged(String oldText, String newText) {
        return !Objects.equals(oldText, newText);
    }

    /**
     * 判断指定行是否变化，行号从 1 开始。
     */
    public static boolean hasLineChanged(String oldText, String newText, int lineNo) {
        if (lineNo < 1) throw new IllegalArgumentException("lineNo 必须从 1 开始");
        List<String> old = lines(oldText), neu = lines(newText);
        return !Objects.equals(lineNo <= old.size() ? old.get(lineNo - 1) : null, lineNo <= neu.size() ? neu.get(lineNo - 1) : null);
    }

    /**
     * 标准化后对比文本。
     */
    public static TextDiff normalizeAndDiffText(String oldText, String newText) {
        return diffText(normalizeText(oldText), normalizeText(newText));
    }

    /**
     * 忽略大小写对比文本。
     */
    public static TextDiff diffTextIgnoreCase(String oldText, String newText) {
        return diffText(str(oldText).toLowerCase(Locale.ROOT), str(newText).toLowerCase(Locale.ROOT));
    }

    /**
     * 忽略首尾空白对比文本。
     */
    public static TextDiff diffTextIgnoreBlank(String oldText, String newText) {
        return diffText(str(oldText).trim(), str(newText).trim());
    }

    /**
     * 忽略换行符对比文本。
     */
    public static TextDiff diffTextIgnoreLineSeparator(String oldText, String newText) {
        return diffText(str(oldText).replaceAll("\\R", ""), str(newText).replaceAll("\\R", ""));
    }

    /**
     * 忽略所有空白字符对比文本。
     */
    public static TextDiff diffTextIgnoreWhitespace(String oldText, String newText) {
        return diffText(SPACE.matcher(str(oldText)).replaceAll(""), SPACE.matcher(str(newText)).replaceAll(""));
    }

    /**
     * 忽略空白行对比文本。
     */
    public static TextDiff diffLinesIgnoreBlank(String oldText, String newText) {
        return diffTokens(lines(oldText).stream().filter(s -> !s.isBlank()).toList(), lines(newText).stream().filter(s -> !s.isBlank()).toList(), "line");
    }

    /**
     * 对比 JSON 字符串。
     */
    public static DiffResult diffJson(String oldJson, String newJson) {
        return diffJsonPath(oldJson, newJson);
    }

    /**
     * 对比 JSON 对象。
     */
    public static DiffResult diffJson(Object oldJson, Object newJson) {
        return diffJsonObjects(oldJson, newJson, false);
    }

    /**
     * 返回 JSONPath 级差异。
     */
    public static DiffResult diffJsonPath(String oldJson, String newJson) {
        return diffJsonObjects(parseJson(oldJson), parseJson(newJson), false);
    }

    /**
     * 获取变化 JSON 路径。
     */
    public static List<String> getChangedJsonPaths(String oldJson, String newJson) {
        return diffJsonPath(oldJson, newJson).items.stream().map(DiffItem::path).toList();
    }

    /**
     * 判断 JSON 是否变化。
     */
    public static boolean hasJsonChanged(String oldJson, String newJson) {
        return diffJsonPath(oldJson, newJson).hasDiff();
    }

    /**
     * 判断指定 JSON 路径是否变化。
     */
    public static boolean hasJsonPathChanged(String oldJson, String newJson, String jsonPath) {
        String p = text(jsonPath, "jsonPath");
        return !jsonEq(readJsonPath(parseJson(oldJson), p), readJsonPath(parseJson(newJson), p), false);
    }

    /**
     * 忽略对象字段顺序对比 JSON。
     */
    public static DiffResult diffJsonIgnoreOrder(String oldJson, String newJson) {
        return diffJsonObjects(parseJson(oldJson), parseJson(newJson), true);
    }

    /**
     * 按数组元素业务 key 对比 JSON 数组。
     */
    public static DiffResult diffJsonArrayByKey(String oldJson, String newJson, String arrayPath, String keyField) {
        String p = text(arrayPath, "arrayPath"), kf = text(keyField, "keyField");
        Object oa = readJsonPath(parseJson(oldJson), p), na = readJsonPath(parseJson(newJson), p);
        if (!(oa instanceof List<?> ol) || !(na instanceof List<?> nl))
            throw new IllegalArgumentException("arrayPath 必须指向数组");
        Map<Object, Object> om = jsonKeyMap(ol, kf), nm = jsonKeyMap(nl, kf);
        List<DiffItem> out = new ArrayList<>();
        for (Object k : nm.keySet()) {
            String cp = p + "[" + kf + "=" + k + "]";
            if (!om.containsKey(k)) out.add(new DiffItem(kf, kf, cp, null, nm.get(k), DiffType.ADDED));
            else jsonDiff(om.get(k), nm.get(k), cp, out, false);
        }
        for (Object k : om.keySet())
            if (!nm.containsKey(k))
                out.add(new DiffItem(kf, kf, p + "[" + kf + "=" + k + "]", om.get(k), null, DiffType.REMOVED));
        return new DiffResult(out);
    }

    /**
     * 扁平化 JSON 差异。
     */
    public static DiffResult flattenJsonDiff(String oldJson, String newJson) {
        return diffJsonPath(oldJson, newJson);
    }

    /**
     * 标准化 JSON 后对比。
     */
    public static DiffResult normalizeJsonAndDiff(String oldJson, String newJson) {
        return diffJson(normalizeJson(oldJson), normalizeJson(newJson));
    }

    /**
     * 对比两个文件，默认 UTF-8 行级对比。
     */
    public static TextDiff diffFile(Path oldFile, Path newFile) {
        return diffFileLines(oldFile, newFile, StandardCharsets.UTF_8);
    }

    /**
     * 按 UTF-8 行级对比文件。
     */
    public static TextDiff diffFileLines(Path oldFile, Path newFile) {
        return diffFileLines(oldFile, newFile, StandardCharsets.UTF_8);
    }

    /**
     * 按指定字符集行级对比文件。
     */
    public static TextDiff diffFileLines(Path oldFile, Path newFile, Charset charset) {
        return diffLines(readText(oldFile, charset), readText(newFile, charset));
    }

    /**
     * 按 UTF-8 文本对比文件。
     */
    public static TextDiff diffFileText(Path oldFile, Path newFile) {
        return diffFileText(oldFile, newFile, StandardCharsets.UTF_8);
    }

    /**
     * 按指定字符集文本对比文件。
     */
    public static TextDiff diffFileText(Path oldFile, Path newFile, Charset charset) {
        return diffText(readText(oldFile, charset), readText(newFile, charset));
    }

    /**
     * 按字节对比文件。
     */
    public static DiffResult diffFileBytes(Path oldFile, Path newFile) {
        byte[] a = readBytes(oldFile), b = readBytes(newFile);
        return Arrays.equals(a, b) ? new DiffResult(List.of()) : new DiffResult(List.of(new DiffItem("bytes", "bytes", "bytes", a.length, b.length, DiffType.MODIFIED)));
    }

    /**
     * 判断文件是否变化。
     */
    public static boolean hasFileChanged(Path oldFile, Path newFile) {
        return hasFileContentChanged(oldFile, newFile);
    }

    /**
     * 判断文件内容是否变化。
     */
    public static boolean hasFileContentChanged(Path oldFile, Path newFile) {
        return !Arrays.equals(readBytes(oldFile), readBytes(newFile));
    }

    /**
     * 按 SHA-256 摘要对比文件。
     */
    public static DiffResult diffFileHash(Path oldFile, Path newFile) {
        DiffItem i = diffValue("sha256", sha256(readBytes(oldFile)), sha256(readBytes(newFile)));
        return i.type == DiffType.UNCHANGED ? new DiffResult(List.of()) : new DiffResult(List.of(i));
    }

    /**
     * 对比两个目录。
     */
    public static DiffResult diffDirectory(Path oldDir, Path newDir) {
        List<DiffItem> out = new ArrayList<>();
        getAddedFiles(oldDir, newDir).forEach(p -> out.add(new DiffItem("file", "file", p.toString(), null, p, DiffType.ADDED)));
        getRemovedFiles(oldDir, newDir).forEach(p -> out.add(new DiffItem("file", "file", p.toString(), p, null, DiffType.REMOVED)));
        getModifiedFiles(oldDir, newDir).forEach(p -> out.add(new DiffItem("file", "file", p.toString(), p, p, DiffType.MODIFIED)));
        return new DiffResult(out);
    }

    /**
     * 获取新增文件。
     */
    public static List<Path> getAddedFiles(Path oldDir, Path newDir) {
        Set<Path> o = relFiles(oldDir), n = relFiles(newDir);
        n.removeAll(o);
        return List.copyOf(n);
    }

    /**
     * 获取删除文件。
     */
    public static List<Path> getRemovedFiles(Path oldDir, Path newDir) {
        return getAddedFiles(newDir, oldDir);
    }

    /**
     * 获取内容变化文件。
     */
    public static List<Path> getModifiedFiles(Path oldDir, Path newDir) {
        Set<Path> o = relFiles(oldDir), n = relFiles(newDir);
        o.retainAll(n);
        List<Path> r = new ArrayList<>();
        for (Path p : o) if (!Arrays.equals(readBytes(oldDir.resolve(p)), readBytes(newDir.resolve(p)))) r.add(p);
        return List.copyOf(r);
    }

    /**
     * 创建文本补丁。
     */
    public static DiffPatch createPatch(String oldText, String newText) {
        List<PatchEntry> e = diffLines(oldText, newText).items.stream().map(i -> new PatchEntry(i.type, i.type == DiffType.REMOVED ? String.valueOf(i.oldValue) : String.valueOf(i.newValue))).toList();
        return new DiffPatch(str(oldText), str(newText), e);
    }

    /**
     * 应用文本补丁。
     */
    public static String applyPatch(String oldText, DiffPatch patch) {
        Objects.requireNonNull(patch, "patch 不能为 null");
        if (!canApplyPatch(oldText, patch)) throw new IllegalArgumentException("补丁不能应用到当前文本");
        return patch.newText;
    }

    /**
     * 反转补丁。
     */
    public static DiffPatch reversePatch(DiffPatch patch) {
        Objects.requireNonNull(patch, "patch 不能为 null");
        return createPatch(patch.newText, patch.oldText);
    }

    /**
     * 合并补丁文本。
     */
    public static String mergePatch(String baseText, String patchText) {
        Objects.requireNonNull(baseText, "baseText 不能为 null");
        return str(patchText);
    }

    /**
     * 判断补丁是否可应用。
     */
    public static boolean canApplyPatch(String oldText, DiffPatch patch) {
        return patch != null && Objects.equals(str(oldText), patch.oldText);
    }

    /**
     * 文本差异转补丁。
     */
    public static DiffPatch diffToPatch(TextDiff diff) {
        Objects.requireNonNull(diff, "diff 不能为 null");
        List<String> o = new ArrayList<>(), n = new ArrayList<>();
        for (DiffItem i : diff.items) {
            if (i.type == DiffType.REMOVED || i.type == DiffType.MODIFIED || i.type == DiffType.UNCHANGED)
                o.add(String.valueOf(i.oldValue));
            if (i.type == DiffType.ADDED || i.type == DiffType.MODIFIED || i.type == DiffType.UNCHANGED)
                n.add(String.valueOf(i.newValue));
        }
        return createPatch(String.join("\n", o), String.join("\n", n));
    }

    /**
     * 补丁转文本差异。
     */
    public static TextDiff patchToDiff(DiffPatch patch) {
        Objects.requireNonNull(patch, "patch 不能为 null");
        return diffLines(patch.oldText, patch.newText);
    }

    /**
     * 生成审计差异。
     */
    public static DiffResult diffForAudit(Object oldBean, Object newBean) {
        return diffBean(oldBean, newBean);
    }

    /**
     * 按选项生成审计差异。
     */
    public static DiffResult diffForAudit(Object oldBean, Object newBean, DiffOptions options) {
        return diffBean(oldBean, newBean, options);
    }

    /**
     * 差异结果转变更日志。
     */
    public static ChangeLog toChangeLog(DiffResult diffResult) {
        return new ChangeLog(null, null, toReadableText(diffResult), DiffType.MODIFIED);
    }

    /**
     * 差异项转变更日志列表。
     */
    public static List<ChangeLog> toChangeLogs(Collection<DiffItem> diffItems) {
        return safe(diffItems).stream().map(i -> new ChangeLog(i.fieldName, i.label, formatChangeLog(i), i.type)).toList();
    }

    /**
     * 差异结果转可读文本。
     */
    public static String toReadableText(DiffResult diffResult) {
        return String.join(System.lineSeparator(), toReadableLines(diffResult));
    }

    /**
     * 差异结果转可读行。
     */
    public static List<String> toReadableLines(DiffResult diffResult) {
        return items(diffResult).stream().map(DiffUtil::formatChangeLog).toList();
    }

    /**
     * 差异结果转字段变化 Map。
     */
    public static Map<String, Pair<Object>> toFieldChangeMap(DiffResult diffResult) {
        Map<String, Pair<Object>> r = new LinkedHashMap<>();
        for (DiffItem i : items(diffResult))
            r.put(i.fieldName != null ? i.fieldName : i.path, new Pair<>(i.oldValue, i.newValue));
        return Map.copyOf(r);
    }

    /**
     * 获取变化字段展示名称。
     */
    public static List<String> getChangedLabels(DiffResult diffResult) {
        return items(diffResult).stream().map(DiffItem::label).filter(Objects::nonNull).toList();
    }

    /**
     * 格式化单个字段变化。
     */
    public static String formatChange(String label, Object oldValue, Object newValue) {
        return str(label) + "：" + show(oldValue) + " -> " + show(newValue);
    }

    /**
     * 格式化单个差异项。
     */
    public static String formatChangeLog(DiffItem item) {
        Objects.requireNonNull(item, "diffItem 不能为 null");
        String l = item.label == null ? item.path : item.label;
        return switch (item.type) {
            case ADDED -> l + "：新增【" + show(item.newValue) + "】";
            case REMOVED -> l + "：删除【" + show(item.oldValue) + "】";
            case MODIFIED -> formatChange(l, item.oldValue, item.newValue);
            case UNCHANGED -> l + "：未变化";
        };
    }

    /**
     * 只保留变化项。
     */
    public static DiffResult filterChanged(DiffResult diffResult) {
        return new DiffResult(items(diffResult).stream().filter(i -> i.type != DiffType.UNCHANGED).toList());
    }

    /**
     * 只保留新增项。
     */
    public static DiffResult filterAdded(DiffResult diffResult) {
        return filterByType(diffResult, DiffType.ADDED);
    }

    /**
     * 只保留删除项。
     */
    public static DiffResult filterRemoved(DiffResult diffResult) {
        return filterByType(diffResult, DiffType.REMOVED);
    }

    /**
     * 只保留修改项。
     */
    public static DiffResult filterModified(DiffResult diffResult) {
        return filterByType(diffResult, DiffType.MODIFIED);
    }

    /**
     * 按字段过滤。
     */
    public static DiffResult filterByField(DiffResult diffResult, Collection<String> fields) {
        Set<String> s = strSet(fields);
        return new DiffResult(items(diffResult).stream().filter(i -> s.contains(i.fieldName)).toList());
    }

    /**
     * 按路径过滤。
     */
    public static DiffResult filterByPath(DiffResult diffResult, Collection<String> paths) {
        Set<String> s = strSet(paths);
        return new DiffResult(items(diffResult).stream().filter(i -> s.contains(i.path)).toList());
    }

    /**
     * 按类型过滤。
     */
    public static DiffResult filterByType(DiffResult diffResult, DiffType type) {
        Objects.requireNonNull(type, "type 不能为 null");
        return new DiffResult(items(diffResult).stream().filter(i -> i.type == type).toList());
    }

    /**
     * 排除字段。
     */
    public static DiffResult excludeFields(DiffResult diffResult, Collection<String> fields) {
        Set<String> s = strSet(fields);
        return new DiffResult(items(diffResult).stream().filter(i -> !s.contains(i.fieldName)).toList());
    }

    /**
     * 排除路径。
     */
    public static DiffResult excludePaths(DiffResult diffResult, Collection<String> paths) {
        Set<String> s = strSet(paths);
        return new DiffResult(items(diffResult).stream().filter(i -> !s.contains(i.path)).toList());
    }

    /**
     * 转为差异 Map。
     */
    public static Map<String, Pair<Object>> toDiffMap(DiffResult diffResult) {
        return toFieldChangeMap(diffResult);
    }

    /**
     * 转为差异项列表。
     */
    public static List<DiffItem> toDiffItems(DiffResult diffResult) {
        return items(diffResult);
    }

    /**
     * 转为摘要。
     */
    public static DiffSummary toSummary(DiffResult diffResult) {
        int a = 0, r = 0, m = 0, u = 0;
        for (DiffItem i : items(diffResult))
            switch (i.type) {
                case ADDED -> a++;
                case REMOVED -> r++;
                case MODIFIED -> m++;
                case UNCHANGED -> u++;
            }
        return new DiffSummary(a, r, m, u, a + r + m + u);
    }

    /**
     * 默认格式化差异。
     */
    public static String format(DiffResult diffResult) {
        return formatAsText(diffResult);
    }

    /**
     * 格式化为文本。
     */
    public static String formatAsText(DiffResult diffResult) {
        return toReadableText(diffResult);
    }

    /**
     * 格式化为多行。
     */
    public static List<String> formatAsLines(DiffResult diffResult) {
        return toReadableLines(diffResult);
    }

    /**
     * 格式化为 Markdown。
     */
    public static String formatAsMarkdown(DiffResult diffResult) {
        StringBuilder b = new StringBuilder("| 字段 | 路径 | 旧值 | 新值 | 类型 |\n|---|---|---|---|---|\n");
        for (DiffItem i : items(diffResult))
            b.append('|').append(str(i.label)).append('|').append(str(i.path)).append('|').append(show(i.oldValue)).append('|').append(show(i.newValue)).append('|').append(i.type).append('|').append('\n');
        return b.toString();
    }

    /**
     * 格式化为 HTML。
     */
    public static String formatAsHtml(DiffResult diffResult) {
        StringBuilder b = new StringBuilder("<table><tbody>");
        for (DiffItem i : items(diffResult))
            b.append("<tr><td>").append(html(str(i.label))).append("</td><td>").append(html(str(i.path))).append("</td><td>").append(html(show(i.oldValue))).append("</td><td>").append(html(show(i.newValue))).append("</td><td>").append(i.type).append("</td></tr>");
        return b.append("</tbody></table>").toString();
    }

    /**
     * 格式化为 JSON。
     */
    public static String formatAsJson(DiffResult diffResult) {
        return items(diffResult).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fieldName", i.fieldName);
            m.put("label", i.label);
            m.put("path", i.path);
            m.put("oldValue", i.oldValue);
            m.put("newValue", i.newValue);
            m.put("type", i.type.name());
            return json(m);
        }).collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * 格式化摘要。
     */
    public static String formatSummary(DiffResult diffResult) {
        DiffSummary s = toSummary(diffResult);
        return "新增：" + s.addedCount + "，删除：" + s.removedCount + "，修改：" + s.modifiedCount + "，未变化：" + s.unchangedCount;
    }

    /**
     * 格式化字段差异。
     */
    public static String formatFieldDiff(FieldDiff fieldDiff) {
        Objects.requireNonNull(fieldDiff, "fieldDiff 不能为 null");
        return formatChange(fieldDiff.label, fieldDiff.oldValue, fieldDiff.newValue);
    }

    /**
     * 格式化集合差异。
     */
    public static String formatCollectionDiff(CollectionDiff<?> collectionDiff) {
        Objects.requireNonNull(collectionDiff, "collectionDiff 不能为 null");
        return "新增：" + collectionDiff.added + "，删除：" + collectionDiff.removed + "，修改：" + collectionDiff.modified + "，未变化：" + collectionDiff.unchanged;
    }

    /**
     * 格式化文本差异。
     */
    public static String formatTextDiff(TextDiff textDiff) {
        Objects.requireNonNull(textDiff, "textDiff 不能为 null");
        return textDiff.items.stream().map(DiffUtil::formatChangeLog).collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * 标准化普通值。
     */
    public static Object normalizeValue(Object value) {
        if (value instanceof String s) return normalizeText(s);
        if (value instanceof BigDecimal b) return b.stripTrailingZeros();
        if (value instanceof Collection<?> c) return normalizeCollection(c);
        if (value instanceof Map<?, ?> m) return normalizeMap(m);
        return value;
    }

    /**
     * 标准化文本。
     */
    public static String normalizeText(String text) {
        return SPACE.matcher(str(text).replaceAll("\\R", "\n").trim()).replaceAll(" ");
    }

    /**
     * 标准化 JSON。
     */
    public static String normalizeJson(String json) {
        return json(parseJson(json));
    }

    /**
     * 标准化集合。
     */
    public static <T> List<Object> normalizeCollection(Collection<T> collection) {
        return safe(collection).stream().map(DiffUtil::normalizeValue).toList();
    }

    /**
     * 标准化 Map。
     */
    public static Map<String, Object> normalizeMap(Map<?, ?> map) {
        Map<String, Object> r = new LinkedHashMap<>();
        safeMap(map).entrySet().stream().sorted(Comparator.comparing(e -> String.valueOf(e.getKey()))).forEach(e -> r.put(String.valueOf(e.getKey()), normalizeValue(e.getValue())));
        return Map.copyOf(r);
    }

    /**
     * 创建忽略 null 选项。
     */
    public static DiffOptions ignoreNull() {
        return DiffOptions.builder().ignoreNull(true).build();
    }

    /**
     * 创建忽略空白选项。
     */
    public static DiffOptions ignoreBlank() {
        return DiffOptions.builder().ignoreBlank(true).build();
    }

    /**
     * 创建忽略大小写选项。
     */
    public static DiffOptions ignoreCase() {
        return DiffOptions.builder().ignoreCase(true).build();
    }

    /**
     * 创建忽略顺序选项。
     */
    public static DiffOptions ignoreOrder() {
        return DiffOptions.builder().ignoreOrder(true).build();
    }

    /**
     * 创建忽略字段选项。
     */
    public static DiffOptions ignoreFields(Collection<String> fields) {
        return DiffOptions.builder().ignoreFields(fields).build();
    }

    /**
     * 创建忽略路径选项。
     */
    public static DiffOptions ignorePaths(Collection<String> paths) {
        return DiffOptions.builder().ignorePaths(paths).build();
    }

    /**
     * 创建忽略时间精度选项。
     */
    public static DiffOptions ignoreTimePrecision(ChronoUnit unit) {
        return DiffOptions.builder().ignoreTimePrecision(unit).build();
    }

    /**
     * 创建忽略数字精度格式选项。
     */
    public static DiffOptions ignoreNumberScale() {
        return DiffOptions.builder().ignoreNumberScale(true).build();
    }

    /**
     * 判断列表顺序是否变化。
     */
    public static <T> boolean hasOrderChanged(List<T> oldList, List<T> newList) {
        return !isSameElementsWithOrder(oldList, newList) && isSameElementsIgnoreOrder(oldList, newList);
    }

    /**
     * 对比顺序变化。
     */
    public static <T> DiffResult diffOrder(List<T> oldList, List<T> newList) {
        List<DiffItem> out = new ArrayList<>();
        for (T t : getMovedItems(oldList, newList))
            out.add(new DiffItem("order", "order", String.valueOf(t), safeList(oldList).indexOf(t), safeList(newList).indexOf(t), DiffType.MODIFIED));
        return new DiffResult(out);
    }

    /**
     * 按 key 对比顺序变化。
     */
    public static <T, K> DiffResult diffOrderByKey(List<T> oldList, List<T> newList, Function<T, K> keyExtractor) {
        List<DiffItem> out = new ArrayList<>();
        for (T t : getMovedItemsByKey(oldList, newList, keyExtractor)) {
            K k = keyExtractor.apply(t);
            out.add(new DiffItem("order", "order", String.valueOf(k), idx(oldList, k, keyExtractor), idx(newList, k, keyExtractor), DiffType.MODIFIED));
        }
        return new DiffResult(out);
    }

    /**
     * 获取位置变化元素。
     */
    public static <T> List<T> getMovedItems(List<T> oldList, List<T> newList) {
        List<T> r = new ArrayList<>();
        List<T> o = safeList(oldList), n = safeList(newList);
        for (T t : n) {
            int oi = o.indexOf(t), ni = n.indexOf(t);
            if (oi >= 0 && oi != ni) r.add(t);
        }
        return List.copyOf(r);
    }

    /**
     * 按 key 获取位置变化元素。
     */
    public static <T, K> List<T> getMovedItemsByKey(List<T> oldList, List<T> newList, Function<T, K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor 不能为 null");
        List<T> r = new ArrayList<>();
        for (T t : safeList(newList)) {
            K k = keyExtractor.apply(t);
            int oi = idx(oldList, k, keyExtractor), ni = idx(newList, k, keyExtractor);
            if (oi >= 0 && oi != ni) r.add(t);
        }
        return List.copyOf(r);
    }

    /**
     * 忽略顺序判断元素是否一致。
     */
    public static boolean isSameElementsIgnoreOrder(Collection<?> oldCollection, Collection<?> newCollection) {
        return addedRaw(oldCollection, newCollection).isEmpty() && addedRaw(newCollection, oldCollection).isEmpty();
    }

    /**
     * 判断元素和顺序是否一致。
     */
    public static boolean isSameElementsWithOrder(List<?> oldList, List<?> newList) {
        return Objects.equals(safeList(oldList), safeList(newList));
    }

    /**
     * 对比树结构。
     */
    public static <T, K> TreeDiff<T> diffTree(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        return diffTreeByKey(oldTree, newTree, keyExtractor);
    }

    /**
     * 按 key 对比树结构。
     */
    public static <T, K> TreeDiff<T> diffTreeByKey(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        CollectionDiff<T> d = diffCollectionByKey(oldTree, newTree, keyExtractor);
        return new TreeDiff<>(d.added, d.removed, d.modified, List.of());
    }

    /**
     * 获取新增节点。
     */
    public static <T, K> List<T> getAddedNodes(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        return diffTreeByKey(oldTree, newTree, keyExtractor).addedNodes;
    }

    /**
     * 获取删除节点。
     */
    public static <T, K> List<T> getRemovedNodes(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        return diffTreeByKey(oldTree, newTree, keyExtractor).removedNodes;
    }

    /**
     * 获取修改节点。
     */
    public static <T, K> List<Pair<T>> getModifiedNodes(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        return diffTreeByKey(oldTree, newTree, keyExtractor).modifiedNodes;
    }

    /**
     * 获取移动节点。
     */
    public static <T, K> List<T> getMovedNodes(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor 不能为 null");
        return List.of();
    }

    /**
     * 判断树是否变化。
     */
    public static <T, K> boolean hasTreeChanged(Collection<T> oldTree, Collection<T> newTree, Function<T, K> keyExtractor) {
        TreeDiff<T> d = diffTreeByKey(oldTree, newTree, keyExtractor);
        return !d.addedNodes.isEmpty() || !d.removedNodes.isEmpty() || !d.modifiedNodes.isEmpty() || !d.movedNodes.isEmpty();
    }

    /**
     * 扁平化树差异。
     */
    public static DiffResult flattenTreeDiff(TreeDiff<?> treeDiff) {
        Objects.requireNonNull(treeDiff, "treeDiff 不能为 null");
        List<DiffItem> out = new ArrayList<>();
        treeDiff.addedNodes.forEach(n -> out.add(new DiffItem("node", "node", String.valueOf(n), null, n, DiffType.ADDED)));
        treeDiff.removedNodes.forEach(n -> out.add(new DiffItem("node", "node", String.valueOf(n), n, null, DiffType.REMOVED)));
        treeDiff.modifiedNodes.forEach(p -> out.add(new DiffItem("node", "node", String.valueOf(p.newValue), p.oldValue, p.newValue, DiffType.MODIFIED)));
        return new DiffResult(out);
    }

    /**
     * 对比两个快照对象。
     */
    public static DiffResult diffSnapshot(Object oldSnapshot, Object newSnapshot) {
        return diffDeep(oldSnapshot, newSnapshot);
    }

    /**
     * 对比两个 JSON 快照。
     */
    public static DiffResult diffSnapshot(String oldSnapshotJson, String newSnapshotJson) {
        return diffJson(oldSnapshotJson, newSnapshotJson);
    }

    /**
     * 判断快照是否变化。
     */
    public static boolean hasSnapshotChanged(Object oldSnapshot, Object newSnapshot) {
        return diffSnapshot(oldSnapshot, newSnapshot).hasDiff();
    }

    /**
     * 创建快照差异。
     */
    public static DiffResult createSnapshotDiff(Object oldData, Object newData) {
        return diffSnapshot(oldData, newData);
    }

    /**
     * 比较快照版本。
     */
    public static DiffResult compareSnapshotVersion(Object baseSnapshot, Object targetSnapshot) {
        return diffSnapshot(baseSnapshot, targetSnapshot);
    }

    /**
     * 对比历史版本。
     */
    public static <T> List<DiffResult> diffHistory(List<T> snapshots, Function<T, ?> versionExtractor) {
        Objects.requireNonNull(versionExtractor, "versionExtractor 不能为 null");
        List<T> s = new ArrayList<>(safeList(snapshots));
        s.sort(Comparator.comparing(x -> String.valueOf(versionExtractor.apply(x))));
        List<DiffResult> r = new ArrayList<>();
        for (int i = 1; i < s.size(); i++) r.add(diffSnapshot(s.get(i - 1), s.get(i)));
        return List.copyOf(r);
    }

    /**
     * 获取最近两次快照差异。
     */
    public static <T> DiffResult getLatestDiff(List<T> snapshots) {
        List<T> s = safeList(snapshots);
        return s.size() < 2 ? new DiffResult(List.of()) : diffSnapshot(s.get(s.size() - 2), s.get(s.size() - 1));
    }

    /**
     * 对比接口响应。
     */
    public static DiffResult diffResponse(Object oldResponse, Object newResponse) {
        return diffDeep(oldResponse, newResponse);
    }

    /**
     * 对比消息载荷。
     */
    public static DiffResult diffPayload(Object oldPayload, Object newPayload) {
        return diffDeep(oldPayload, newPayload);
    }

    /**
     * 对比同步数据。
     */
    public static <T, K> CollectionDiff<T> diffSyncData(Collection<T> sourceData, Collection<T> targetData, Function<T, K> keyExtractor) {
        return diffCollectionByKey(targetData, sourceData, keyExtractor);
    }

    /**
     * 获取需要插入的数据。
     */
    public static <T, K> List<T> getNeedInsert(Collection<T> sourceData, Collection<T> targetData, Function<T, K> keyExtractor) {
        return diffSyncData(sourceData, targetData, keyExtractor).added;
    }

    /**
     * 获取需要更新的数据。
     */
    public static <T, K> List<Pair<T>> getNeedUpdate(Collection<T> sourceData, Collection<T> targetData, Function<T, K> keyExtractor) {
        return diffSyncData(sourceData, targetData, keyExtractor).modified;
    }

    /**
     * 获取需要删除的数据。
     */
    public static <T, K> List<T> getNeedDelete(Collection<T> sourceData, Collection<T> targetData, Function<T, K> keyExtractor) {
        return diffSyncData(sourceData, targetData, keyExtractor).removed;
    }

    /**
     * 判断同步数据是否有差异。
     */
    public static <T, K> boolean hasSyncDiff(Collection<T> sourceData, Collection<T> targetData, Function<T, K> keyExtractor) {
        CollectionDiff<T> d = diffSyncData(sourceData, targetData, keyExtractor);
        return !d.added.isEmpty() || !d.removed.isEmpty() || !d.modified.isEmpty();
    }


    private static List<Object> addedRaw(Collection<?> oldCollection, Collection<?> newCollection) {
        List<Object> old = new ArrayList<>(safe(oldCollection));
        List<Object> r = new ArrayList<>();
        for (Object n : safe(newCollection)) if (!old.remove(n)) r.add(n);
        return r;
    }

    private static Set<Object> addedKeysRaw(Map<?, ?> oldMap, Map<?, ?> newMap) {
        Set<Object> r = new LinkedHashSet<>(safeMap(newMap).keySet());
        r.removeAll(safeMap(oldMap).keySet());
        return r;
    }

    private static Set<Object> changedKeysRaw(Map<?, ?> oldMap, Map<?, ?> newMap) {
        Set<Object> r = new LinkedHashSet<>();
        Map<?, ?> old = safeMap(oldMap), neu = safeMap(newMap);
        for (Object k : old.keySet()) if (neu.containsKey(k) && !Objects.equals(old.get(k), neu.get(k))) r.add(k);
        return r;
    }

    private static DiffType type(Object oldValue, Object newValue, boolean same) {
        if (same) return DiffType.UNCHANGED;
        if (oldValue == null) return DiffType.ADDED;
        if (newValue == null) return DiffType.REMOVED;
        return DiffType.MODIFIED;
    }

    private static String text(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return v;
    }

    private static String str(String v) {
        return v == null ? "" : v;
    }

    private static <T> Collection<T> safe(Collection<T> c) {
        return c == null ? List.of() : c;
    }

    private static <T> List<T> safeList(List<T> c) {
        return c == null ? List.of() : c;
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> m) {
        return m == null ? Map.of() : m;
    }

    private static List<DiffItem> items(DiffResult r) {
        return r == null ? List.of() : r.items;
    }

    private static Set<String> strSet(Collection<String> v) {
        return v == null ? new LinkedHashSet<>() : v.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static void requireBeans(Object o, Object n) {
        if (o == null || n == null) throw new IllegalArgumentException("oldBean 和 newBean 不能为 null");
        sameClass(o, n);
    }

    private static void sameClass(Object o, Object n) {
        if (o != null && n != null && !o.getClass().equals(n.getClass()))
            throw new IllegalArgumentException("对象类型不一致");
    }

    private static boolean simple(Object v) {
        return v == null || v instanceof CharSequence || v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof Enum<?> || v instanceof Temporal;
    }

    private static Set<String> fieldNames(Class<?> type, DiffOptions opt) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Field f : fields(type, opt)) names.add(f.getName());
        return names;
    }

    private static List<Field> fields(Class<?> type, DiffOptions opt) {
        List<Field> fs = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (f.isSynthetic()) continue;
                if (opt.ignoreStatic && Modifier.isStatic(m)) continue;
                if (opt.ignoreTransient && Modifier.isTransient(m)) continue;
                fs.add(f);
            }
        return fs;
    }

    private static boolean compareField(String f, String p, DiffOptions o) {
        return (o.includeFields.isEmpty() || o.includeFields.contains(f)) && !o.ignoreFields.contains(f) && !o.ignorePaths.contains(p);
    }

    private static Object fieldValue(Object bean, String name) {
        if (bean == null) return null;
        Field f = findField(bean.getClass(), name);
        if (f == null) throw new IllegalArgumentException("字段不存在：" + name);
        try {
            f.setAccessible(true);
            return f.get(bean);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("字段不可访问：" + name, e);
        }
    }

    private static Field findField(Class<?> c, String n) {
        for (Class<?> x = c; x != null && x != Object.class; x = x.getSuperclass())
            try {
                return x.getDeclaredField(n);
            } catch (NoSuchFieldException ignored) {
            }
        return null;
    }

    private static boolean sameByOption(String f, String p, Object o, Object n, DiffOptions opt) {
        BiPredicate<Object, Object> c = opt.fieldComparators.getOrDefault(f, opt.fieldComparators.get(p));
        if (c != null) return c.test(o, n);
        if (opt.ignoreNull && (o == null || n == null)) return true;
        if (opt.ignoreNewNull && n == null) return true;
        if (opt.ignoreOldNull && o == null) return true;
        if (o == null || n == null) return o == n;
        if (opt.ignoreBlank && o instanceof CharSequence os && n instanceof CharSequence ns && os.toString().isBlank() && ns.toString().isBlank())
            return true;
        if (opt.ignoreCase && o instanceof String os && n instanceof String ns) return os.equalsIgnoreCase(ns);
        if (opt.ignoreNumberScale && o instanceof Number on && n instanceof Number nn)
            return decimal(on).compareTo(decimal(nn)) == 0;
        if (opt.ignoreTimePrecision != null && o instanceof Temporal ot && n instanceof Temporal nt)
            return Objects.equals(trunc(ot, opt.ignoreTimePrecision), trunc(nt, opt.ignoreTimePrecision));
        if (opt.ignoreOrder && o instanceof Collection<?> oc && n instanceof Collection<?> nc)
            return isSameElementsIgnoreOrder(oc, nc);
        return Objects.equals(o, n);
    }

    private static BigDecimal decimal(Number n) {
        return n instanceof BigDecimal b ? b : new BigDecimal(n.toString());
    }

    private static Object trunc(Temporal t, ChronoUnit u) {
        if (t instanceof LocalDateTime v) return v.truncatedTo(u);
        if (t instanceof LocalTime v) return v.truncatedTo(u);
        if (t instanceof OffsetDateTime v) return v.truncatedTo(u);
        if (t instanceof ZonedDateTime v) return v.truncatedTo(u);
        return t;
    }

    private static void deep(Object o, Object n, String p, List<DiffItem> out, DiffOptions opt, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (sameByOption(last(p), p, o, n, opt)) return;
        if (depth > opt.maxDepth || o == null || n == null || simple(o) || simple(n)) {
            out.add(new DiffItem(last(p), last(p), p, o, n, type(o, n, false)));
            return;
        }
        if (visited.containsKey(o) || visited.containsKey(n)) return;
        visited.put(o, true);
        visited.put(n, true);
        if (o instanceof Map<?, ?> om && n instanceof Map<?, ?> nm) {
            LinkedHashSet<Object> ks = new LinkedHashSet<>();
            ks.addAll(om.keySet());
            ks.addAll(nm.keySet());
            for (Object k : ks) {
                String cp = append(p, String.valueOf(k));
                if (!opt.ignorePaths.contains(cp)) deep(om.get(k), nm.get(k), cp, out, opt, depth + 1, visited);
            }
            return;
        }
        if (o instanceof List<?> ol && n instanceof List<?> nl) {
            for (int i = 0, max = Math.max(ol.size(), nl.size()); i < max; i++)
                deep(i < ol.size() ? ol.get(i) : null, i < nl.size() ? nl.get(i) : null, p + "[" + i + "]", out, opt, depth + 1, visited);
            return;
        }
        if (o instanceof Collection<?> oc && n instanceof Collection<?> nc) {
            if (!isSameElementsIgnoreOrder(oc, nc)) out.add(new DiffItem(last(p), last(p), p, o, n, DiffType.MODIFIED));
            return;
        }
        if (!o.getClass().equals(n.getClass())) {
            out.add(new DiffItem(last(p), last(p), p, o, n, DiffType.MODIFIED));
            return;
        }
        for (String f : fieldNames(o.getClass(), opt))
            if (compareField(f, append(p, f), opt))
                deep(fieldValue(o, f), fieldValue(n, f), append(p, f), out, opt, depth + 1, visited);
    }

    private static String append(String p, String c) {
        return p == null || p.isBlank() ? c : ROOT.equals(p) ? ROOT + "." + c : p + "." + c;
    }

    private static String last(String p) {
        if (p == null) return null;
        int i = p.lastIndexOf('.');
        return i >= 0 ? p.substring(i + 1) : p;
    }

    private static Object readPath(Object root, String p) {
        Object cur = root;
        String x = p.startsWith(ROOT + ".") ? p.substring(2) : p;
        if (ROOT.equals(p)) return root;
        for (String seg : x.split("\\.")) {
            if (cur == null) return null;
            cur = readSeg(cur, seg);
        }
        return cur;
    }

    private static Object readSeg(Object cur, String seg) {
        String name = seg;
        Integer idx = null;
        int b = seg.indexOf('[');
        if (b >= 0 && seg.endsWith("]")) {
            name = seg.substring(0, b);
            idx = Integer.parseInt(seg.substring(b + 1, seg.length() - 1));
        }
        Object v = name.isEmpty() ? cur : cur instanceof Map<?, ?> m ? m.get(name) : fieldValue(cur, name);
        if (idx != null) {
            if (!(v instanceof List<?> l)) throw new IllegalArgumentException("路径段不是列表：" + seg);
            return idx >= 0 && idx < l.size() ? l.get(idx) : null;
        }
        return v;
    }

    private static <T, K> Map<K, T> keyMap(Collection<T> c, Function<T, K> f) {
        Objects.requireNonNull(f, "keyExtractor 不能为 null");
        Map<K, T> r = new LinkedHashMap<>();
        for (T t : safe(c)) {
            if (t == null) continue;
            K k = f.apply(t);
            if (k == null) throw new IllegalArgumentException("keyExtractor 不能返回 null");
            r.put(k, t);
        }
        return r;
    }

    private static List<String> chars(String s) {
        return str(s).chars().mapToObj(c -> String.valueOf((char) c)).toList();
    }

    private static List<String> words(String s) {
        String v = str(s).trim();
        return v.isEmpty() ? List.of() : Arrays.asList(v.split("\\s+"));
    }

    private static List<String> lines(String s) {
        String v = str(s);
        return v.isEmpty() ? List.of() : Arrays.asList(v.split("\\R", -1));
    }

    private static TextDiff diffTokens(List<String> o, List<String> n, String prefix) {
        List<DiffItem> out = new ArrayList<>();
        for (int i = 0, max = Math.max(o.size(), n.size()); i < max; i++) {
            String ov = i < o.size() ? o.get(i) : null, nv = i < n.size() ? n.get(i) : null;
            if (!Objects.equals(ov, nv))
                out.add(new DiffItem(prefix, prefix, prefix + "[" + (i + 1) + "]", ov, nv, type(ov, nv, false)));
        }
        return new TextDiff(out);
    }

    private static DiffResult diffJsonObjects(Object o, Object n, boolean ignoreOrder) {
        List<DiffItem> out = new ArrayList<>();
        jsonDiff(o, n, ROOT, out, ignoreOrder);
        return new DiffResult(out);
    }

    private static void jsonDiff(Object o, Object n, String p, List<DiffItem> out, boolean ignoreOrder) {
        if (jsonEq(o, n, ignoreOrder)) return;
        if (o instanceof Map<?, ?> om && n instanceof Map<?, ?> nm) {
            LinkedHashSet<Object> ks = new LinkedHashSet<>();
            ks.addAll(om.keySet());
            ks.addAll(nm.keySet());
            for (Object k : ks) jsonDiff(om.get(k), nm.get(k), p + "." + k, out, ignoreOrder);
            return;
        }
        if (o instanceof List<?> ol && n instanceof List<?> nl) {
            if (ignoreOrder && jsonEq(ol, nl, true)) return;
            for (int i = 0, max = Math.max(ol.size(), nl.size()); i < max; i++)
                jsonDiff(i < ol.size() ? ol.get(i) : null, i < nl.size() ? nl.get(i) : null, p + "[" + i + "]", out, ignoreOrder);
            return;
        }
        out.add(new DiffItem(last(p), last(p), p, o, n, type(o, n, false)));
    }

    private static boolean jsonEq(Object o, Object n, boolean ignoreOrder) {
        if (o instanceof BigDecimal ob && n instanceof BigDecimal nb) return ob.compareTo(nb) == 0;
        if (ignoreOrder && o instanceof List<?> ol && n instanceof List<?> nl)
            return ol.stream().map(DiffUtil::json).sorted().toList().equals(nl.stream().map(DiffUtil::json).sorted().toList());
        return Objects.equals(o, n);
    }

    private static Object parseJson(String s) {
        try {
            return new Parser(str(s)).parse();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("JSON 格式错误：" + e.getMessage(), e);
        }
    }

    private static Object readJsonPath(Object root, String p) {
        if (ROOT.equals(p)) return root;
        Object cur = root;
        String x = p.startsWith(ROOT + ".") ? p.substring(2) : p;
        for (String seg : splitPath(x)) {
            if (cur == null) return null;
            int b = seg.indexOf('[');
            String name = b >= 0 ? seg.substring(0, b) : seg;
            if (!name.isEmpty()) {
                if (!(cur instanceof Map<?, ?> m)) throw new IllegalArgumentException("JSON 路径不是对象节点");
                cur = m.get(name);
            }
            while (b >= 0) {
                int e = seg.indexOf(']', b);
                int idx = Integer.parseInt(seg.substring(b + 1, e));
                if (!(cur instanceof List<?> l)) throw new IllegalArgumentException("JSON 路径不是数组节点");
                cur = idx >= 0 && idx < l.size() ? l.get(idx) : null;
                b = seg.indexOf('[', e);
            }
        }
        return cur;
    }

    private static List<String> splitPath(String p) {
        List<String> r = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        int d = 0;
        for (char c : p.toCharArray()) {
            if (c == '[') d++;
            else if (c == ']') d--;
            if (c == '.' && d == 0) {
                r.add(b.toString());
                b.setLength(0);
            } else b.append(c);
        }
        if (!b.isEmpty()) r.add(b.toString());
        return r;
    }

    private static Map<Object, Object> jsonKeyMap(List<?> l, String key) {
        Map<Object, Object> r = new LinkedHashMap<>();
        for (Object v : l) {
            if (!(v instanceof Map<?, ?> m)) throw new IllegalArgumentException("数组元素必须是对象");
            Object k = m.get(key);
            if (k == null) throw new IllegalArgumentException("数组元素缺少 key 字段");
            r.put(k, v);
        }
        return r;
    }

    private static String json(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + esc(s) + "\"";
        if (v instanceof Character c) return "\"" + esc(String.valueOf(c)) + "\"";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Enum<?> e) return "\"" + e.name() + "\"";
        if (v instanceof Map<?, ?> m)
            return m.entrySet().stream().sorted(Comparator.comparing(e -> String.valueOf(e.getKey()))).map(e -> "\"" + esc(String.valueOf(e.getKey())) + "\":" + json(e.getValue())).collect(Collectors.joining(",", "{", "}"));
        if (v instanceof Collection<?> c)
            return c.stream().map(DiffUtil::json).collect(Collectors.joining(",", "[", "]"));
        return "\"" + esc(String.valueOf(v)) + "\"";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String readText(Path p, Charset c) {
        file(p);
        try {
            return Files.readString(p, c == null ? StandardCharsets.UTF_8 : c);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败：" + p, e);
        }
    }

    private static byte[] readBytes(Path p) {
        file(p);
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败：" + p, e);
        }
    }

    private static void file(Path p) {
        if (p == null || !Files.isRegularFile(p)) throw new IllegalArgumentException("不是有效文件：" + p);
    }

    private static void dir(Path p) {
        if (p == null || !Files.isDirectory(p)) throw new IllegalArgumentException("不是有效目录：" + p);
    }

    private static Set<Path> relFiles(Path d) {
        dir(d);
        try (Stream<Path> s = Files.walk(d)) {
            return s.filter(Files::isRegularFile).map(d::relativize).collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            throw new IllegalArgumentException("遍历目录失败：" + d, e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder b = new StringBuilder();
            for (byte x : h) b.append(String.format("%02x", x));
            return b.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    private static String show(Object v) {
        if (v == null) return "null";
        if (v instanceof byte[] b) return Arrays.toString(b);
        if (v instanceof Object[] a) return Arrays.toString(a);
        return String.valueOf(v);
    }

    private static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static <T, K> int idx(List<T> list, K key, Function<T, K> f) {
        List<T> s = safeList(list);
        for (int i = 0; i < s.size(); i++) if (Objects.equals(f.apply(s.get(i)), key)) return i;
        return -1;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s.trim();
        }

        Object parse() {
            if (s.isEmpty()) throw new IllegalArgumentException("JSON 不能为空");
            Object v = value();
            ws();
            if (i != s.length()) throw new IllegalArgumentException("存在多余字符");
            return v;
        }

        Object value() {
            ws();
            if (i >= s.length()) throw new IllegalArgumentException("缺少值");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> obj();
                case '[' -> arr();
                case '"' -> string();
                case 't' -> lit("true", true);
                case 'f' -> lit("false", false);
                case 'n' -> lit("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) yield num();
                    throw new IllegalArgumentException("非法字符：" + c);
                }
            };
        }

        Map<String, Object> obj() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            ws();
            if (peek('}')) {
                i++;
                return m;
            }
            while (true) {
                String k = string();
                expect(':');
                m.put(k, value());
                ws();
                if (peek('}')) {
                    i++;
                    return m;
                }
                expect(',');
            }
        }

        List<Object> arr() {
            expect('[');
            List<Object> l = new ArrayList<>();
            ws();
            if (peek(']')) {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                ws();
                if (peek(']')) {
                    i++;
                    return l;
                }
                expect(',');
            }
        }

        String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    if (i >= s.length()) throw new IllegalArgumentException("转义不完整");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> b.append('"');
                        case '\\' -> b.append('\\');
                        case '/' -> b.append('/');
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'n' -> b.append('\n');
                        case 'r' -> b.append('\r');
                        case 't' -> b.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw new IllegalArgumentException("Unicode 转义不完整");
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw new IllegalArgumentException("非法转义");
                    }
                } else b.append(c);
            }
            throw new IllegalArgumentException("字符串未闭合");
        }

        BigDecimal num() {
            int st = i;
            if (peek('-')) i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            if (peek('.')) {
                i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            return new BigDecimal(s.substring(st, i));
        }

        Object lit(String l, Object v) {
            if (!s.startsWith(l, i)) throw new IllegalArgumentException("非法字面量");
            i += l.length();
            return v;
        }

        void expect(char c) {
            ws();
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("期望字符：" + c);
            i++;
        }

        boolean peek(char c) {
            return i < s.length() && s.charAt(i) == c;
        }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
