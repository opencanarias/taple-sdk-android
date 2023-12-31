#!/bin/bash

# Compilation mode: release/debug
mode="release"

# Architectures to compile. Comment on those you do not want to compile
architectures=()
# architectures+=("rust_target android_target")
architectures+=("aarch64-linux-android arm64-v8a")
architectures+=("x86_64-linux-android x86_64")
architectures+=("i686-linux-android x86")

lib_name="libtaple_ffi.so"

scripts_dir=$(pwd)
ffi_dir=$scripts_dir/../../taple-ffi
sdk_dir=$scripts_dir/../sdk

cd $ffi_dir

for key in ${!architectures[@]}
do
    read -a value_array <<< "${architectures[$key]}"

    rust_target=${value_array[0]}
    android_target=${value_array[1]}

    lib_path=target/$rust_target/$mode/$lib_name
    lib_final_path=$sdk_dir/TapleSDK/src/main/jniLibs/$android_target

    echo "Compiling architecture: $rust_target/$android_target"
    cross build --features android --target $rust_target --$mode

    echo "Copying $lib_name($android_target) to $lib_final_path"
    mkdir -p $lib_final_path
    cp $lib_path $lib_final_path
done

echo "Generating Kotling bindings"
udl_path=$ffi_dir/src/taple_uniffi.udl
package_dir=$sdk_dir/TapleSDK/src/main/java
cargo run --bin uniffi-bindgen generate $udl_path --out-dir $package_dir --language kotlin
