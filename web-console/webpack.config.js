/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       webpack开发环境配置
 * */

const webpack = require('webpack');
const HappyPack = require('happypack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const config = require('./config');

const proxy = process.env.DEV_PROXY || '192.168.32.101';

module.exports = {
  mode: 'development',
  resolve: {
    extensions: ['.js'],
    modules: ['node_modules'],
  },
  entry: config.path.entry,
  devServer: {
    hot: true,
    contentBase: config.root,
    port: config.webpack.dev.devServer.port,
    host: '0.0.0.0',
    publicPath: '/',
    historyApiFallback: true,
    disableHostCheck: true,
    proxy: {
      '/api': {
        target: `https://unidemo.dcloud.net.cn`,
        changeOrigin: true,
      },
    },
  },
  output: {
    path: config.path.distPath,
    publicPath: config.webpack.publicPath,
    filename: 'app.[hash].js',
  },
  devtool: 'cheap-module-eval-source-map',
  module: {
    rules: [
      {
        test: /\.js$/,
        use: 'happypack/loader?id=babel',
        include: config.path.srcPath,
        exclude: config.path.nodeModulesPath,
      },
      {
        test: /\.less|css$/,
        use: [
          {
            loader: 'style-loader',
          },
          {
            loader: 'css-loader',
          },
          {
            loader: 'less-loader',
            options: {
              // less@3
              javascriptEnabled: true,
              // 覆盖antd样式的全局变量
              modifyVars: config.webpack.modifyVars,
            },
          },
        ],
      },
      // 处理图片(file-loader来处理也可以，url-loader更适合图片)
      {
        test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: 'static/assets/images/[name].[hash:7].[ext]',
        },
      },
      // 处理多媒体文件
      {
        test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: 'static/assets/media/[name].[hash:7].[ext]',
        },
      },
      // 处理字体文件
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: 'static/assets/fonts/[name].[hash:7].[ext]',
        },
      },
    ],
  },
  plugins: [
    // 多进程
    new HappyPack({
      id: 'babel',
      loaders: ['babel-loader'],
    }),
    // 热更新
    new webpack.HotModuleReplacementPlugin(),
    // html模板
    new HtmlWebpackPlugin({
      hash: false,
      template: config.path.indexHtml,
      title: 'react-template',
    }),
  ],
};
