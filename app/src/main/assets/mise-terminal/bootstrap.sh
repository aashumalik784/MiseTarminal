#!/system/bin/sh
GREEN=$'\033[1;32m'
YELLOW=$'\033[1;33m'
RED=$'\033[1;31m'
RESET=$'\033[0m'

BASE_DIR="$1"
MARKER="$BASE_DIR/.mise-initialized"
ROOTFS_DIR="$BASE_DIR/rootfs"
BIN_DIR="$BASE_DIR/bin"
PROOT_BIN="$BIN_DIR/proot"

if [ -f "$MARKER" ]; then
    echo "${GREEN}Mise Tarminal already configured!${RESET}"
    exit 0
fi

mkdir -p "$BIN_DIR"
mkdir -p "$ROOTFS_DIR"

if [ ! -x "$PROOT_BIN" ]; then
    echo "${YELLOW}Setting up proot engine...${RESET}"
    cp "$BASE_DIR/assets_copy/proot-arm64" "$PROOT_BIN"
    cp "$BASE_DIR/assets_copy/libtalloc.so.2" "$BIN_DIR/"
    cp "$BASE_DIR/assets_copy/libandroid-shmem.so" "$BIN_DIR/"
    chmod +x "$PROOT_BIN"
fi

export LD_LIBRARY_PATH="$BIN_DIR"

if [ ! -d "$ROOTFS_DIR/bin" ]; then
    echo "${YELLOW}Downloading Ubuntu base rootfs...${RESET}"
    ROOTFS_TAR="$BASE_DIR/ubuntu-base.tar.gz"
    wget -q -O "$ROOTFS_TAR" "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04-base-arm64.tar.gz"
    mkdir -p "$ROOTFS_DIR"
    tar -xf "$ROOTFS_TAR" -C "$ROOTFS_DIR"
    rm -f "$ROOTFS_TAR"
    echo "${GREEN}Rootfs ready!${RESET}"
fi

echo "${YELLOW}Installing development tools (this takes a while)...${RESET}"
"$PROOT_BIN" --link2symlink -0 -r "$ROOTFS_DIR" \
    -b /dev -b /proc -b /sys \
    -w /root /usr/bin/env -i \
    HOME=/root \
    PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    /bin/bash -c "
        echo 'nameserver 8.8.8.8' > /etc/resolv.conf
        apt update -y
        DEBIAN_FRONTEND=noninteractive apt install -y python3 openjdk-21-jdk g++ nodejs npm php golang-go rustc ruby kotlin nginx mariadb-server postgresql wget curl unzip
    "

touch "$MARKER"
echo "${GREEN}Mise Tarminal Setup Complete!${RESET}"
