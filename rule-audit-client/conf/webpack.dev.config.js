const common = require("./webpack.common.config.js");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const webpack = require("webpack");

const definePlugin = new webpack.DefinePlugin({
  ENV: JSON.stringify("dev")
});

module.exports = {
  ...common,
  mode: "development",
  devtool: "cheap-module-eval-source-map",
  devServer: {
    hot: true
  },
  plugins: [...common.plugins, new HtmlWebpackPlugin(), definePlugin]
};
