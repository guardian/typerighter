{ sources ? import ./nix/sources.nix }:
let
  pkgs = import sources.nixpkgs { };
  # guardianNix = builtins.fetchGit {
  #   url = "git@github.com:guardian/guardian-nix.git";
  #   ref = "refs/tags/v1";
  # };
  # guardianDev = import "${guardianNix.outPath}/guardian-dev.nix" pkgs;
  guardianDev =
    import /Users/emily_bourke/code/guardian-nix/guardian-dev.nix pkgs;
  yarnWithNode20 = pkgs.yarn.override { nodejs = pkgs.nodejs_20; };

in guardianDev.devEnv {
  name = "typerighter";
  commands = [ ];
  extraInputs = [ pkgs.nodejs_20 yarnWithNode20 pkgs.sbt pkgs.metals ];
}
