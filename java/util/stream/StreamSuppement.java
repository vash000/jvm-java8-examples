
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface StreamsSupplement {

    static <T,U,R> Stream<R> zip(final Iterable<? extends T> first, final Iterable<? extends U> second, final BiFunction<? super T,? super U,? extends R> func) {
        return StreamSupport.stream(ZipIterator.from(first,second,func),false);
    }

    static <T,U,R> Stream<R> zip(final Collection<? extends T> first, final Collection<? extends U> second, final BiFunction<? super T,? super U,? extends R> func) {
        return StreamSupport.stream(ZipIterator.from(first,second,func),false);
    }

    static <T,U,R> Stream<R> zip(final Stream<? extends T> first, final Stream<? extends U> second, final BiFunction<? super T,? super U,? extends R> func) {
        return StreamSupport.stream(ZipSpliterator.from(first,second,func),first.isParallel() || second.isParallel());
    }


    class ZipIterator<T,U,R> implements Iterator<R> {

        static <T,U,R> Spliterator<R> from(final Iterable<? extends T> first, final Iterable<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            return Spliterators.spliteratorUnknownSize(
                    new ZipIterator<>(first,second,func)
                    ,Spliterator.NONNULL | Spliterator.ORDERED);
        }

        static <T,U,R> Spliterator<R> from(final Collection<? extends T> first, final Collection<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            final int size = Math.min(first.size(),second.size());
            return size == 0 ? Spliterators.emptySpliterator() :
                    Spliterators.spliterator(
                            new ZipIterator<>(first,second,func)
                            ,size
                            ,Spliterator.NONNULL | Spliterator.ORDERED);
        }


        private final Iterable<? extends T> lhs;
        private final Iterable<? extends U> rhs;
        private final BiFunction<? super T, ? super U, ? extends R> zip;

        private Iterator<? extends T> t;
        private Iterator<? extends U> u;


        ZipIterator(final Iterable<? extends T> first, final Iterable<? extends U> second, final BiFunction<? super T, ? super U, ? extends R> func) {
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
                t = lhs.iterator();
                u = rhs.iterator();
            }
        }
    }

    class ZipSpliterator<T,U,R> implements Spliterator<R> {
        static <T,U,R> Spliterator<R> from(final Stream<? extends T> first, final Stream<? extends U> second, final BiFunction<? super T, ? super U,? extends R> func) {
            final Spliterator<? extends T> lhs = Objects.requireNonNull(first).spliterator();
            final Spliterator<? extends U> rhs = Objects.requireNonNull(second).spliterator();
            return from(lhs,rhs,func);
        }

        static <T, U, R> Spliterator<R> from(Spliterator<? extends T> lefts, Spliterator<? extends U> rights, BiFunction<? super T, ? super U, ? extends R> func) {
            return new ZipSpliterator<>(lefts, rights, func);
        }

        private final Spliterator<? extends T> lhs;
        private final Spliterator<? extends U> rhs;
        private final BiFunction<? super T, ? super U, ? extends R> func;

        private boolean advancedOnRight;
        ZipSpliterator(Spliterator<? extends T> lhs, Spliterator<? extends U> rhs, BiFunction<? super T, ? super U, ? extends R> func) {
            this.lhs = Objects.requireNonNull(lhs);
            this.rhs = Objects.requireNonNull(rhs);
            this.func = Objects.requireNonNull(func);
        }

        @Override
        public boolean tryAdvance(Consumer<? super R> action) {
            return lhs.tryAdvance( t -> {
                this.advancedOnRight = rhs.tryAdvance( u -> action.accept(func.apply(t,u)));
            }) && this.advancedOnRight;
        }

        @Override
        public Spliterator<R> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return ((characteristics() & Spliterator.SIZED) != 0)
                    ? Math.min(lhs.getExactSizeIfKnown(), rhs.getExactSizeIfKnown())
                    : -1;
        }

        @Override
        public int characteristics() {
            return lhs.characteristics() & rhs.characteristics() &
                    ~(Spliterator.DISTINCT | Spliterator.SORTED); // Zipping looses DISTINCT and SORTED characteristics
        }
    }
}