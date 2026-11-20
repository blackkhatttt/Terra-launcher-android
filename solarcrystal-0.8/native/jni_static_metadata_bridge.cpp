#include <jni.h>
#include "class_file_model.h"
#include <cstdint>
#include <exception>
#include <sstream>
#include <string>
#include <vector>

extern "C" JNIEXPORT jstring JNICALL
Java_com_universe_solarcrystal_NativeBridge_nativeInspectStaticMetadata(
    JNIEnv* env, jclass, jbyteArray class_bytes) {
    if (class_bytes == nullptr) {
        return env->NewStringUTF("SolarCrystal JNI: ERROR\\nClass byte array is null");
    }
    const jsize length = env->GetArrayLength(class_bytes);
    if (length <= 0 || length > 16 * 1024 * 1024) {
        return env->NewStringUTF("SolarCrystal JNI: ERROR\\nClass file is empty or exceeds 16 MiB");
    }
    std::vector<std::uint8_t> bytes(static_cast<std::size_t>(length));
    env->GetByteArrayRegion(class_bytes, 0, length, reinterpret_cast<jbyte*>(bytes.data()));
    if (env->ExceptionCheck()) return nullptr;
    try {
        const auto model = solarcrystal::ClassFileModel::parse(bytes.data(), bytes.size());
        std::ostringstream out;
        out << "SolarCrystal JNI: OK\\nClass: " << model.name()
            << "\\nStatic fields:";
        std::size_t count = 0;
        for (const auto& field : model.fields()) {
            if (!field.is_static()) continue;
            ++count;
            out << "\\n- " << field.name << " " << field.descriptor;
            if (field.has_constant_value()) {
                const auto& value = model.constant_value(field);
                out << " [ConstantValue tag=" << static_cast<unsigned>(value.tag)
                    << ", bits=" << value.bits << "]";
            } else {
                out << " [mutable/default]";
            }
        }
        out << "\\nStatic field count: " << count;
        const auto* initializer = model.class_initializer();
        out << "\\nClass initializer: " << (initializer ? "present (not executed)" : "absent");
        const auto result = out.str();
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& error) {
        const std::string result = std::string("SolarCrystal JNI: ERROR\\n") + error.what();
        return env->NewStringUTF(result.c_str());
    }
}
