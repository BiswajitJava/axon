#!/bin/sh
# Axon CLI Installer Script

set -e

# Define constants
REPO="your-username/axon-cli" # <--- UPDATE THIS
BINARY_NAME="axon"
INSTALL_DIR="/usr/local/bin"

# Fetch the latest release version from GitHub API
get_latest_release() {
  curl --silent "https://api.github.com/repos/${REPO}/releases/latest" | # Get latest release
    grep '"tag_name":' |                                                 # Get tag line
    sed -E 's/.*"([^"]+)".*/\1/'                                         # Pluck JSON value
}

# Identify OS and architecture
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)
case $ARCH in
    x86_64) ARCH="amd64" ;;
    aarch64 | arm64) ARCH="arm64" ;;
    *)
        echo "Error: Unsupported architecture: $ARCH"
        exit 1
        ;;
esac

# Check for supported OS
if [ "$OS" != "linux" ]; then
    echo "Error: This installer is for Linux only. For macOS, use Homebrew."
    exit 1
fi

# Construct the download URL
LATEST_VERSION=$(get_latest_release)
if [ -z "$LATEST_VERSION" ]; then
    echo "Error: Could not fetch the latest version. Check repository name."
    exit 1
fi

DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${LATEST_VERSION}/axon-cli-ubuntu-latest-${ARCH}"
INSTALL_PATH="${INSTALL_DIR}/${BINARY_NAME}"

# Perform installation
echo "Downloading Axon CLI ${LATEST_VERSION} for Linux (${ARCH})..."
if ! curl -Lo "/tmp/${BINARY_NAME}" "$DOWNLOAD_URL"; then
    echo "Error: Failed to download the binary. Please check the URL and your connection."
    exit 1
fi

echo "Installing to ${INSTALL_PATH} (requires sudo)..."
chmod +x "/tmp/${BINARY_NAME}"
if ! sudo mv "/tmp/${BINARY_NAME}" "$INSTALL_PATH"; then
    echo "Error: Failed to move binary to ${INSTALL_DIR}. Check permissions."
    exit 1
fi

echo ""
echo "✅ Axon CLI was installed successfully!"
echo "Run 'axon' to get started."