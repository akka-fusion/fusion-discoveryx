import React, { useEffect, useState, useRef } from 'react';
import * as PropTypes from 'prop-types';
import './index.less';

const DEFAULT_ELLIPSIS = '...';
const TOGGLE_ON_TEXT = '展开';
const TOGGLE_OFF_TEXT = '收起';

const TOGGLE_STATUS = {
  OFF: 0,
  ON: 1,
};

const TOGGLE_VISIBILITY = {
  HIDDEN: 0,
  SHOW: 1,
};

const Clamp = props => {
  const { text, style, maxLines, ellipsis, toggle, onText, offText } = props;
  const rootRef = useRef(null);
  const contentRef = useRef(null);
  const textRef = useRef(null);
  const [toggleStatus, setToggleStatus] = useState(TOGGLE_STATUS.OFF);
  const [toggleVisibility, setToggleVisibility] = useState(TOGGLE_VISIBILITY.SHOW);
  const [localMaxLines, setLocalMaxLines] = useState(maxLines);
  let offset = null;

  const getLines = () => contentRef.current.getClientRects().length;

  const isOverflow = () => {
    if (!localMaxLines && !style.maxHeight) {
      return false;
    }

    if (localMaxLines) {
      if (getLines() > localMaxLines) {
        return true;
      }
    }

    if (style.maxHeight) {
      if (rootRef.current.scrollHeight > rootRef.current.offsetHeight) {
        return true;
      }
    }
    return false;
  };

  const clampedText = () => text.slice(0, offset) + ellipsis;

  const isClamped = () => {
    if (!text) {
      return false;
    }
    return offset !== text.length;
  };

  const getRealText = () => (isClamped() ? clampedText() : text);

  const applyChange = () => {
    if (textRef.current) {
      textRef.current.textContent = getRealText();
    }
  };

  const clampAt = _offset => {
    offset = _offset;
    applyChange();
  };

  const moveEdge = steps => {
    clampAt(offset + steps);
  };

  const fill = () => {
    while (!isOverflow() && offset < text.length) {
      moveEdge(1);
    }
  };

  const clamp = () => {
    while (isOverflow() && offset > 0) {
      moveEdge(-1);
    }
  };

  const stepToFit = () => {
    fill();
    clamp();
    // setRealText(getRealText());
  };

  const search = (from, to) => {
    if (to - from <= ellipsis.length) {
      stepToFit();
      return;
    }
    // 从中间找临界值
    const target = Math.floor((to + from) / 2);
    clampAt(target);
    if (isOverflow()) {
      search(from, target);
    } else {
      search(target, to);
    }
  };

  useEffect(() => {
    setLocalMaxLines(maxLines);
  }, [maxLines]);

  useEffect(() => {
    if (!isOverflow() && localMaxLines !== Infinity) {
      setToggleVisibility(TOGGLE_VISIBILITY.HIDDEN);
    } else {
      setToggleVisibility(TOGGLE_VISIBILITY.SHOW);
    }
    search(0, text.length);
  }, [text, localMaxLines]);

  return (
    <div className="clamp-container" style={style} ref={rootRef}>
      <span ref={contentRef}>
        <span className="clamp-container-text" ref={textRef}>
          {text}
        </span>
        {toggle && toggleVisibility === TOGGLE_VISIBILITY.SHOW && (
          <button
            onClick={() => {
              setLocalMaxLines(toggleStatus === TOGGLE_STATUS.OFF ? Infinity : maxLines);
              setToggleStatus(
                toggleStatus === TOGGLE_STATUS.ON ? TOGGLE_STATUS.OFF : TOGGLE_STATUS.ON,
              );
            }}
            type="button"
            className="clamp-container-btn"
          >
            {toggleStatus === TOGGLE_STATUS.ON ? offText : onText}
          </button>
        )}
      </span>
    </div>
  );
};

Clamp.propTypes = {
  text: PropTypes.string.isRequired,
  maxLines: PropTypes.number.isRequired,
  style: PropTypes.object,
  toggle: PropTypes.bool,
  ellipsis: PropTypes.string,
  onText: PropTypes.string,
  offText: PropTypes.string,
};

Clamp.defaultProps = {
  style: {
    width: '100%',
  },
  toggle: true,
  ellipsis: DEFAULT_ELLIPSIS,
  onText: TOGGLE_ON_TEXT,
  offText: TOGGLE_OFF_TEXT,
};

export default Clamp;
