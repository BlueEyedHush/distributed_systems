package knawara.zad2.client;

public interface TriConsumer<T, U, W> {
    void accept(T first, U second, W thrid);
}
