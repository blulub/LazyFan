package Pollers;

public interface Poller<T> {
  T doPoll();
}
