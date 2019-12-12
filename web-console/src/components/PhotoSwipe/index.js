/**
 * Date              Author           Des
 *----------------------------------------------
 * 2018/7/24           gongtiexin       响应式图片查看器
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';
import PropTypes from 'prop-types';
import shortid from 'shortid';
import 'photoswipe/dist/photoswipe.css';
import 'photoswipe/dist/default-skin/default-skin.css';
import './index.less';
import initPhotoSwipeFromDOM from './init';

const dataSize = '1024x1024';
const description = '';

@observer
export default class PhotoSwipe extends Component {
  static propTypes = {
    images: PropTypes.array.isRequired,
  };

  state = {
    num: 0,
  };

  componentDidMount() {}

  componentDidUpdate() {
    if (this.props.images.length !== 0 && this.state.num === 0) {
      initPhotoSwipeFromDOM('.my-gallery');
      this.setState({ num: 1 });
    }
  }

  componentWillUnmount() {}

  renderImageListItem = () =>
    this.props.images.map(url => (
      <figure
        itemProp="associatedMedia"
        itemScope
        itemType="http://schema.org/ImageObject"
        key={shortid.generate()}
      >
        <a href={url} itemProp="contentUrl" data-size={dataSize}>
          <img src={url} itemProp="thumbnail" alt={description} />
        </a>
        <figcaption itemProp="caption description">{description}</figcaption>
      </figure>
    ));

  render() {
    return (
      <div id="photoSwipe">
        <div className="my-gallery" itemScope itemType="http://schema.org/ImageGallery">
          {this.renderImageListItem()}
        </div>
        <div className="pswp" tabIndex="-1" role="dialog" aria-hidden="true">
          <div className="pswp__bg" />
          <div className="pswp__scroll-wrap">
            <div className="pswp__container">
              <div className="pswp__item" />
              <div className="pswp__item" />
              <div className="pswp__item" />
            </div>
            <div className="pswp__ui pswp__ui--hidden">
              <div className="pswp__top-bar">
                <div className="pswp__counter" />
                <button className="pswp__button pswp__button--close" title="Close (Esc)" />
                <button className="pswp__button pswp__button--share" title="Share" />
                <button className="pswp__button pswp__button--fs" title="Toggle fullscreen" />
                <button className="pswp__button pswp__button--zoom" title="Zoom in/out" />
                <div className="pswp__preloader">
                  <div className="pswp__preloader__icn">
                    <div className="pswp__preloader__cut">
                      <div className="pswp__preloader__donut" />
                    </div>
                  </div>
                </div>
              </div>
              <div className="pswp__share-modal pswp__share-modal--hidden pswp__single-tap">
                <div className="pswp__share-tooltip" />
              </div>
              <button
                className="pswp__button pswp__button--arrow--left"
                title="Previous (arrow left)"
              />
              <button
                className="pswp__button pswp__button--arrow--right"
                title="Next (arrow right)"
              />
              <div className="pswp__caption">
                <div className="pswp__caption__center" />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
