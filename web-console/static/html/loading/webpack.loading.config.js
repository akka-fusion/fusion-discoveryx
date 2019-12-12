const path = require("path");
const webpack = require("webpack");
const CopyWebpackPlugin = require("copy-webpack-plugin");

module.exports = {
  entry: {
    loading: ["babel-polyfill", path.join(__dirname, "loading")],
  },
  output: {
    path: path.join(__dirname, "../../../dist/static/html/loading"),
    filename: "[name].js",
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        include: path.join(__dirname),
        loader: "babel-loader",
      },
      {
        test: /\.css$/,
        include: path.join(__dirname),
        use: "css-loader",
      },
    ],
  },
  plugins: [
    new webpack.optimize.UglifyJsPlugin({
      uglifyOptions: {
        minimize: true,
        ie8: false,
        output: {
          comments: false,
          beautify: false,
        },
        mangle: {
          keep_fnames: true,
        },
        compress: {
          warnings: false,
          // drop_console: true,
          drop_debugger: true,
          unused: true,
        },
      },
    }),
    new CopyWebpackPlugin([
      {
        from: path.resolve(__dirname),
        to: path.join(__dirname, "../../../dist/static/html/loading"),
        ignore: ["*.js"],
      },
    ]),
  ],
};
