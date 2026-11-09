{ pkgs ? import <nixpkgs> { config = { allowUnfree = true; android_sdk.accept_license = true; }; } }:

let
  # Use only essential SDK components (no emulator images)
  androidSdkComposition = pkgs.androidenv.composeAndroidPackages {
    toolsVersion = "26.1.1";
    platformToolsVersion = "35.0.2";
    buildToolsVersions = [ "34.0.0" ];
    platformVersions = [ "34" ];
    includeNDK = false;
    includeEmulator = false;
    includeSystemImages = false;
  };
in
pkgs.mkShell {
  buildInputs = with pkgs; [
    # Build tools
    gradle
    openjdk17
    
    # Utilities
    git
    wget
    unzip
    
    # Android SDK
    androidSdkComposition
  ];
  
  # Set JAVA_HOME
  JAVA_HOME = "${pkgs.openjdk17}";
  
  # Gradle opts for memory
  GRADLE_OPTS = "-Xmx2048m";
  
  # Set ANDROID_HOME to the Nix-provided Android SDK
  ANDROID_HOME = "${androidSdkComposition}/libexec/android-sdk";
  
  # Add Android SDK to PATH
  shellHook = ''
    export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"
    echo "Android SDK: $ANDROID_HOME"
    echo "JAVA_HOME: $JAVA_HOME"
  '';
}
