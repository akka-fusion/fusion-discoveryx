import {
  Observable,
  Subject,
  asapScheduler,
  pipe,
  of,
  from,
  interval,
  merge,
  fromEvent,
  SubscriptionLike,
  PartialObserver,
} from 'rxjs';
import { map, filter, scan } from 'rxjs/operators';
import { ajax } from 'rxjs/ajax';

const main = () => {
  const onSubscribe = observer => {
    observer.next(1);
    observer.next(2);
    observer.next(3);
  };
  const source$ = new Observable(onSubscribe);
  const observer = {
    next: item => console.log(item),
  };
  source$.subscribe(observer);

  const a = {};
  console.log(a.a);

  const next = x => console.log(x);
  console.log(12 |> next);
  console.log([1, 2, 3].map(item => item * 2));
};

export default main;
