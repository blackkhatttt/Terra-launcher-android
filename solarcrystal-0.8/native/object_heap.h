#pragma once

#include <cstddef>
#include <cstdint>
#include <map>
#include <string>
#include <vector>

namespace solarcrystal {

using ObjectId = std::uint32_t;

struct ObjectField {
    std::string name;
    std::int32_t initial_value = 0;
};

class ObjectHeap {
public:
    static constexpr std::size_t MAX_OBJECTS = 4096;

    ObjectHeap() = default;

    ObjectId allocate(const std::string& class_name,
                      const std::vector<ObjectField>& fields);
    std::int32_t get_int(ObjectId object_id,
                         const std::string& class_name,
                         const std::string& field_name) const;
    void put_int(ObjectId object_id,
                 const std::string& class_name,
                 const std::string& field_name,
                 std::int32_t value);
    bool is_valid(ObjectId object_id) const;
    std::size_t size() const;

private:
    struct Instance {
        std::string class_name;
        std::map<std::string, std::int32_t> int_fields;
    };

    std::vector<Instance> objects_;
    const Instance& checked_instance(ObjectId object_id,
                                     const std::string& class_name) const;
    Instance& checked_instance(ObjectId object_id,
                                const std::string& class_name);
};

} // namespace solarcrystal
