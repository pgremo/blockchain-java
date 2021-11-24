{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/931ab058daa7e4cd539533963f95e2bb0dbd41e6.tar.gz") {}
}:let

in pkgs.mkShell {

  buildInputs = with pkgs; [
    git
    jdk
    maven
  ];

}