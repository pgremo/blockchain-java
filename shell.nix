{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/81cef6b70fb5d5cdba5a0fef3f714c2dadaf0d6d.tar.gz") {}
}:let

in pkgs.mkShell {

  buildInputs = with pkgs; [
    git
    jdk17_headless
    maven
  ];

}
