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
  yarnWithNode18 = pkgs.yarn.override { nodejs = pkgs.nodejs_18; };

in guardianDev.devEnv {
  name = "typerighter";
  commands = [ ];
  extraInputs = [ pkgs.nodejs_18 yarnWithNode18 pkgs.sbt ];
}
