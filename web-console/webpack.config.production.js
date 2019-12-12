/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       webpack生产环境配置
 * */

const webpack = require('webpack');
const HappyPack = require('happypack');
const postcssPresetEnv = require('postcss-preset-env');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const InlineManifestWebpackPlugin = require('inline-manifest-webpack-plugin');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
// const { BundleAnalyzerPlugin } = require("webpack-bundle-analyzer");
const MomentLocalesPlugin = require('moment-locales-webpack-plugin');

const config = require('./config');

module.exports = {
  mode: 'production',
  resolve: {
    extensions: ['.js'],
    modules: ['node_modules'],
  },
  entry: {
    app: config.path.entry,
  },
  output: {
    path: config.path.distPath,
    publicPath: config.webpack.publicPath,
    filename: 'assets/[name].[chunkhash].js',
    chunkFilename: 'assets/[name].[chunkhash].chunk.js',
  },
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
          MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'postcss-loader',
            options: {
              ident: 'postcss',
              plugins: () => [postcssPresetEnv(/* pluginOptions */)],
            },
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
  optimization: {
    minimizer: [
      // 自定义js优化配置，将会覆盖默认配置
      new UglifyJsPlugin({
        // 过滤掉以".min.js"结尾的文件，默认这个后缀本身就是已经压缩好的代码，没必要进行二次压缩
        exclude: /\.min\.js$/,
        cache: true,
        // 多进程并行运行来提高构建速度
        parallel: true,
        sourceMap: false,
        // 移除注释
        extractComments: false,
        uglifyOptions: {
          compress: {
            unused: true,
            drop_console: true,
          },
          warnings: false,
          output: {
            comments: false,
          },
        },
      }),
      // 用于优化css文件
      new OptimizeCSSAssetsPlugin({
        assetNameRegExp: /\.css$/g,
        cssProcessorOptions: {
          // safe: true,
          autoprefixer: { disable: true },
          mergeLonghand: false,
          discardComments: {
            // 移除注释
            removeAll: true,
          },
        },
        canPrint: true,
      }),
    ],
    runtimeChunk: 'single',
    splitChunks: {
      chunks: 'all',
      automaticNameDelimiter: '.',
      name: undefined,
      cacheGroups: {
        default: false,
        vendors: false,
        common: {
          test(module, chunks) {
            // 这里通过配置规则只将多个页面引用的打包进 common 中
            if (
              // /src\/common\//.test(module.context) ||
              // /src\/lib/.test(module.context) ||
              /antd/.test(module.context)
            ) {
              return true;
            }
          },
          chunks: 'all',
          name: 'common',
          // 这里的minchunks 非常重要，控antd使用的组件被超过几个chunk引用之后才打包进入该common中否则不打包进该js中
          minChunks: 2,
          priority: 20,
        },
        vendor: {
          chunks: 'all',
          test: (module, chunks) => {
            // 将node_modules 目录下的依赖统一打包进入vendor中
            if (/node_modules/.test(module.context)) {
              return true;
            }
          },
          name: 'vendor',
          minChunks: 2,
          // 配置chunk的打包优先级，这里的数值决定了node_modules下的 antd 不会打包进入 vendor 中
          priority: 10,
          enforce: true,
        },
      },
    },
  },
  plugins: [
    // 多进程
    new HappyPack({
      id: 'babel',
      loaders: ['babel-loader'],
    }),
    // 分析打包的结构
    // new BundleAnalyzerPlugin(),
    new MiniCssExtractPlugin({
      filename: 'stylesheets/[name].[contenthash].css',
      chunkFilename: 'stylesheets/[id].[contenthash].css',
    }),
    // 使得哈希基于模块的相对路径, 生成一个四个字符的字符串作为模块ID
    new webpack.HashedModuleIdsPlugin(),
    // html模板
    new HtmlWebpackPlugin({
      filename: 'index.html',
      template: config.path.indexHtml,
      title: 'react-template',
    }),
    new InlineManifestWebpackPlugin('runtime'),
    // 拷贝静态资源
    new CopyWebpackPlugin(config.webpack.build.plugins.CopyWebpackPlugin),
    // 去除moment中除“zh-cn”之外的所有语言环境, “en”内置于Moment中，不能删除
    new MomentLocalesPlugin({
      localesToKeep: ['zh-cn'],
    }),
  ],
};
