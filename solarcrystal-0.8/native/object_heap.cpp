#include "object_heap.h"

#include <limits>
#include <stdexcept>
#include <utility>

namespace solarcrystal {

ObjectId ObjectHeap::allocate(const std::string& class_name,
                              const std::vector<ObjectField>& fields) {
    if (class_name.empty())
        throw std::runtime_error("Object allocation requires a class name");
    if (objects_.size() >= MAX_OBJECTS)
        throw std::runtime_error("Object heap limit exceeded");

    Instance instance;
    instance.class_name = class_name;
    for (const auto& field : fields) {
        if (field.name.empty())
            throw std::runtime_error("Object field name cannot be empty");
        if (!instance.int_fields.emplace(field.name, field.initial_value).second)
            throw std::runtime_error("Duplicate object field: " + field.name);
    }
    if (objects_.size() >= std::numeric_limits<ObjectId>::max() - 1)
        throw std::runtime_error("Object identifier space exhausted");
    objects_.push_back(std::move(instance));
    return static_cast<ObjectId>(objects_.size());
}

const ObjectHeap::Instance& ObjectHeap::checked_instance(
    ObjectId object_id, const std::string& class_name) const {
    if (object_id == 0 || object_id > objects_.size())
        throw std::runtime_error("Null or invalid object reference");
    const auto& instance = objects_[object_id - 1];
    if (instance.class_name != class_name)
        throw std::runtime_error("Object class identity mismatch");
    return instance;
}

ObjectHeap::Instance& ObjectHeap::checked_instance(
    ObjectId object_id, const std::string& class_name) {
    if (object_id == 0 || object_id > objects_.size())
        throw std::runtime_error("Null or invalid object reference");
    auto& instance = objects_[object_id - 1];
    if (instance.class_name != class_name)
        throw std::runtime_error("Object class identity mismatch");
    return instance;
}

std::int32_t ObjectHeap::get_int(ObjectId object_id,
                                 const std::string& class_name,
                                 const std::string& field_name) const {
    const auto& instance = checked_instance(object_id, class_name);
    const auto field = instance.int_fields.find(field_name);
    if (field == instance.int_fields.end())
        throw std::runtime_error("Unknown int instance field: " + field_name);
    return field->second;
}

void ObjectHeap::put_int(ObjectId object_id,
                         const std::string& class_name,
                         const std::string& field_name,
                         std::int32_t value) {
    auto& instance = checked_instance(object_id, class_name);
    const auto field = instance.int_fields.find(field_name);
    if (field == instance.int_fields.end())
        throw std::runtime_error("Unknown int instance field: " + field_name);
    field->second = value;
}

void ObjectHeap::define_int_field(ObjectId object_id,
                                  const std::string& field_name,
                                  std::int32_t initial_value) {
    if (object_id == 0 || object_id > objects_.size())
        throw std::runtime_error("Null or invalid object reference");
    if (field_name.empty())
        throw std::runtime_error("Object field name cannot be empty");
    auto& fields = objects_[object_id - 1].int_fields;
    if (!fields.emplace(field_name, initial_value).second)
        throw std::runtime_error("Duplicate object field: " + field_name);
}

std::string ObjectHeap::class_name(ObjectId object_id) const {
    if (object_id == 0 || object_id > objects_.size())
        throw std::runtime_error("Null or invalid object reference");
    return objects_[object_id - 1].class_name;
}

bool ObjectHeap::is_valid(ObjectId object_id) const {
    return object_id != 0 && object_id <= objects_.size();
}

std::size_t ObjectHeap::size() const { return objects_.size(); }

} // namespace solarcrystal
