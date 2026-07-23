#!/system/bin/sh
GREEN=$'\033[1;32m'
YELLOW=$'\033[1;33m'
RED=$'\033[1;31m'
RESET=$'\033[0m'

BASE_DIR="$1"
NATIVE_DIR="$2"
MARKER="$BASE_DIR/.mise-initialized"
ROOTFS_DIR="$BASE_DIR/rootfs"
ROOTFS_TAR="$BASE_DIR/ubuntu-base.tar.gz"
PROOT_BIN="$NATIVE_DIR/libproot.so"

if [ -f "$MARKER" ]; then
    echo "${GREEN}Mise Tarminal already configured!${RESET}"
    exit 0
fi

export LD_LIBRARY_PATH="$NATIVE_DIR"
mkdir -p "$ROOTFS_DIR"

if [ ! -x "$PROOT_BIN" ]; then
    echo "${RED}proot engine not found at $PROOT_BIN${RESET}"
    exit 1
fi

if [ ! -d "$ROOTFS_DIR/bin" ]; then
    if [ ! -f "$ROOTFS_TAR" ]; then
        echo "${RED}Rootfs archive not downloaded yet!${RESET}"
        exit 1
    fi
    echo "${YELLOW}Extracting Ubuntu rootfs...${RESET}"
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
        DEBIAN_FRONTEND=noninteractive apt install -y python3 openjdk-21-jdk g++ nodejs npm php golang-go rustc ruby kotlin nginx mariadb-server postgresql curl unzip
    "

touch "$MARKER"
echo "${GREEN}Mise Tarminal Setup Complete!${RESET}"
