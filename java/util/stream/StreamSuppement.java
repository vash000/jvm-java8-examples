import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

interface StreamsSupplement {

    static <T,U,R> Stream<R> zip(final Iterable<? extends T> first, final Iterable<? extends U> second, final BiFunction<? super T,? super U,? extends R> func) {
        return StreamSupport.stream(ZipIterator.from(first,second,func),false);
    }

    class ZipIterator<T,U,R> implements Iterator<R> {

        private final Supplier<Iterator<? extends T>> lhs;
        private final Supplier<Iterator<? extends U>> rhs;
        private final BiFunction<? super T, ? super U, ? extends R> zip;

        private Iterator<? extends T> t;
        private Iterator<? extends U> u;

        static <T,U,R> Spliterator<R> from(final Iterable<? extends T> first, final Iterable<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            return Spliterators.spliteratorUnknownSize(
                        new ZipIterator<>(first::iterator,second::iterator,func)
                    ,Spliterator.NONNULL | Spliterator.ORDERED);
        }

        static <T,U,R> Spliterator<R> from(final Collection<? extends T> first, final Collection<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            final int size = Math.min(first.size(),second.size());
            return size == 0 ? Spliterators.emptySpliterator() :
                    Spliterators.spliterator(
                        new ZipIterator<>(first::iterator,second::iterator,func)
                        ,size
                        ,Spliterator.NONNULL | Spliterator.ORDERED);
        }

        static <T,U,R> Spliterator<R> from(final Stream<? extends T> first, final Stream<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            final Spliterator<? extends T> lhs = Objects.requireNonNull(first).spliterator();
            final Spliterator<? extends U> rhs = Objects.requireNonNull(second).spliterator();


            final int characteristics = lhs.characteristics() & rhs.characteristics() &
                    ~(Spliterator.DISTINCT | Spliterator.SORTED); // Zipping looses DISTINCT and SORTED characteristics

            final long size = ((characteristics & Spliterator.SIZED) != 0)
                    ? Math.min(lhs.getExactSizeIfKnown(), rhs.getExactSizeIfKnown())
                    : -1;

            final Iterator<R> iterator = new ZipIterator<>(
                    () -> Spliterators.iterator(lhs),
                    () -> Spliterators.iterator(rhs),
                    func);

            return Spliterators.spliterator(iterator, size, characteristics);
        }

        ZipIterator(final Supplier<Iterator<? extends T>> first, final Supplier<Iterator<? extends U>> second, final BiFunction<? super T, ? super U, ? extends R> func) {
            this.lhs = Objects.requireNonNull(first);
            this.rhs = Objects.requireNonNull(second);
            this.zip = Objects.requireNonNull(func);
        }


        @Override
        public boolean hasNext() {
            init();
            return t.hasNext() && u.hasNext();
        }

        @Override
        public R next() {
            return zip.apply(t.next(),u.next());
        }

        private void init() {
            if (t == null && u == null) {
                t = Objects.requireNonNull(lhs.get());
                u = Objects.requireNonNull(rhs.get());
            }
        }
    }
}