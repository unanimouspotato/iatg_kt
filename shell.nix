{ pkgs ? import <nixpkgs> {}
}:
pkgs.mkShell {
  name="dev-env";
  buildInputs = [
    pkgs.jdk8
    pkgs.yt-dlp
    pkgs.python39Full
    pkgs.python39Packages.spotipy
    pkgs.cacert
  ];
  shellHook = ''
    echo "Start developing..."
  '';
}