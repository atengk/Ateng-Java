package io.github.atengk.diff;

record AddressData(String city, String street) {
}

record UserData(Long id, String name, Integer age, AddressData address) {
}

record ItemData(Long id, String name, Integer sort) {
}

record NodeData(Long id, String name, Long parentId) {
}

record VersionData(Integer version, String value) {
}
