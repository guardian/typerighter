const common = require("./webpack.common.config.js");
const webpack = require("webpack");

const definePlugin = new webpack.DefinePlugin({
  ENV: JSON.stringify("prod")
});

module.exports = {
  ...common,
  mode: "production",
  devtool: "source-map",
  plugins: [...common.plugins, definePlugin]
};
