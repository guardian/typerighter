const common = require("./webpack.common.config.js");

module.exports =  {
  ...common,
  mode: "production",
  devtool: "source-map"
};
