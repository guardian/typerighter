const common = require("./webpack.common.config.js");

module.exports = {
  ...common,
  mode: "development",
  devtool: "cheap-module-eval-source-map",
  devServer: {
    hot: true,
    public: 'assets.typerighter.local.dev-gutools.co.uk',
    allowedHosts: [
      '.dev-gutools.co.uk'
  ]
  },
  plugins: [...common.plugins]
};
