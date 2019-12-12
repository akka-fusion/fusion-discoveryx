/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       高亮
 * */

import React, { Fragment } from 'react';
import shortid from 'shortid';
import * as PropTypes from 'prop-types';

const Highlight = ({ text, keyword }) => {
  const reg = new RegExp(keyword, 'gi');
  const match = text.match(reg);
  if (!match) {
    return text;
  }
  return (
    <span>
      {text.split(reg).map((fragment, i) =>
        i > 0 ? (
          <Fragment key={shortid.generate()}>
            <em className="fe-highlight">{match[0]}</em>
            {fragment}
          </Fragment>
        ) : (
          fragment
        ),
      )}
    </span>
  );
};

Highlight.propTypes = {
  text: PropTypes.string.isRequired,
  keyword: PropTypes.string.isRequired,
};

export default Highlight;
