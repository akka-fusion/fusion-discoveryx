import React from 'react';
import { Redirect, Route } from 'react-router-dom';
import PropTypes from 'prop-types';

// class RecordRoute extends PureComponent {
//   // static propTypes = {};
//
//   // constructor(props) {
//   //   super(props);
//   // }
//
//   componentDidMount() {}
//
//   componentWillUnmount() {}
//
//   render() {
//     return <Route {...this.props} />;
//   }
// }
//
// const PrivateRoute = ({
//                         component: RouteComponent,
//                         loggedIn,
//                         protect,
//                         ...rest
//                       }) => (
//   <RecordRoute
//     {...rest}
//     render={props =>
//       loggedIn || !protect ? (
//         <RouteComponent {...props} />
//       ) : (
//         <Redirect
//           to={{
//             pathname: "/login",
//           }}
//         />
//       )
//     }
//   />
// );

const PrivateRoute = ({ component: Component, isAuthenticated, ...rest }) => {
  return (
    <Route
      {...rest}
      render={props =>
        isAuthenticated ? (
          <Component {...props} />
        ) : (
          <Redirect
            to={{
              pathname: '/login',
              // eslint-disable-next-line react/prop-types
              state: { from: props.location },
            }}
          />
        )
      }
    />
  );
};

PrivateRoute.propTypes = {
  component: PropTypes.func.isRequired,
  isAuthenticated: PropTypes.bool,
};

PrivateRoute.defaultProps = {
  isAuthenticated: true,
};

export { PrivateRoute };
