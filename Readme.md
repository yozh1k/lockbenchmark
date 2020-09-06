#### Synchronized vs Lock

При обстоятельствах, детали которых не так важны, был сформулирован следующий тезис. Использование ключевого слова synchronized дает выигрыш в производительности по сравнению с java.util.concurrent.locks.Lock.

Мне показалось это утверждение спорным и я решил проверить его. Я нашел статьи датированные 2011 годом, авторы которых были солидарны со мной, но на дворе 2020 год и я решил проверить на практике, можно ли по крайней мере однозначно соглашаться или не соглашаться с озвученным утверждением.

Для этого я написал простейший потоконебезопасный счетчик:

```java
public class NonThreadSafeCounter {
    private volatile int i = 0;
    public void increment() {
        i++;
    }
}
```

Для тестирования я подготовил 3 способа синхронного вызова счетчика:
* с использованием synchronized-секции
```java
    private void doMeasure() {
        synchronized (lock) {
            counter.increment();
        }
    }
```
* с использованием synchronized-блока
```java
    private class SynchronizedCounterWrapper {
        private synchronized void doMeasure() {
            counter.increment();
        }
    }
```
* с использованием ReentrantLock'а
 ```java
    private ReentrantLock lock;
    private NonThreadSafeCounter counter;
    private void doMeasure() {
        lock.lock();
        counter.increment();
        lock.unlock();
    }
 ```
 Затем для каждого из способов синхронизации при помощи фреймворка jmh я создал 3 метода-бенчмарка, которые отличались количеством потоков, а именно: однопоточный вариант, для 2х потоков и для количества потоков, эквивалентного числу доступных ядер.

 Итоговый бенчмарк для ReentrantLock я приведу ниже, остальные сделаны аналогично.
```java
@State(Scope.Benchmark)
public class LockBenchmark {
    private ReentrantLock lock;
    private NonThreadSafeCounter counter;

    @Setup(Level.Iteration)
    public void setUpCounter() {
        lock = new ReentrantLock();
        counter = new NonThreadSafeCounter();
    }

    private void doMeasure() {
        lock.lock();
        counter.increment();
        lock.unlock();
    }

    @Threads(1)
    @Benchmark
    public void singleThreadMeasure() {
        doMeasure();
    }

    @Threads(2)
    @Benchmark
    public void twoThreadMeasure() {
        doMeasure();
    }

    @Threads(8)
    @Benchmark
    public void coreCountThreadMeasure() {
        doMeasure();
    }
}
```


Затем я прогнал получившиеся бенчмарки на своем компьютере.
Конфигурация тестового окружения: Linux Mint 19 Cinnamon 3.8.9, Intel Core i7-3770 CPU @ 3.40GHz × 4

В качестве JVM я использовал JDK 1.8.0_252. И получил следующие результаты для количества операций в секунду.

| | LockBenchmark	| SynchronizedMethodBenchmark | SynchronizedSectionBenchmark |
| -------------|:---------------:|:-------------:|:-----:|
| coreCountThreadMeasure | 13139774,377 | 9233990,876 | 8895153,208 |
| singleThreadMeasure | 49469746,932 | 94795184,779 | 137784882,204 |
| twoThreadMeasure | 7955459,563 | 15201228,719  | 15421543,665  |

В виде накопительной гистрограммы это выглядит так:

![picture](https://raw.githubusercontent.com/yozh1k/lockbenchmark/master/results/8/8.png)

Результаты получились неоднозначные. Действительно, для небольшого количества конкурирующих потоков или вовсе в отсутствие  конкуреции synchronized работает быстрее. Причем разница в способе использования synchronized для однопоточного варианта оказывается довольно существенной. Однако при увеличении числа конкурирующих потоков оказывается вперёд уже вырывается ReentrantLock.

Я дополнительно прогнал те же измерения с использованием других версий JDK.

Результаты 11 версии
| | LockBenchmark	| SynchronizedMethodBenchmark | SynchronizedSectionBenchmark |
| ------------- |:-------------:|:-------------:|:-----:|
| coreCountThreadMeasure | 12687540,493 | 9387418,715 | 9893503,33 |
| singleThreadMeasure | 48614869,449  | 138317014,948 | 137726815,486 |
| twoThreadMeasure | 8153936,961  | 22756867,238 | 24366107,333 |

![picture](https://raw.githubusercontent.com/yozh1k/lockbenchmark/master/results/11/11.png)
Здесь результаты примерно те же за исключением того, что разница при различных способах использования synchronized практически ичезает.

Результаты 13 версии
| | LockBenchmark	| SynchronizedMethodBenchmark | SynchronizedSectionBenchmark |
| ------------- |:-------------:|:-------------:|:-----:|
| coreCountThreadMeasure | 11920392,211 | 9381171,059 | 9751548,69 |
| singleThreadMeasure | 48648991,035  | 138401909,855 | 138389513,615 |
| twoThreadMeasure | 7975837,493  | 28299790,093 | 27811727,651 |

![picture](https://raw.githubusercontent.com/yozh1k/lockbenchmark/master/results/13/13.png)

Опять же, результаты примерно те же, но преимущество в скорости при использовании Lock с большим
количеством потоков немного сокращается

Таким образом, независимо от весрии jdk результаты были приблизительно аналогичными. Причины такого поведения кроются в том, что JDK использует 4 типа локов [https://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/geninfo/diagnos/thread_basics.html]:
* тонкие - локи, за которые отсутствует конкуренция между несколькими потоками
* толстые  - локи, за которые есть конкуренция между несколькими потоками
* рекурсивные  - локи, которые захватываются многократно одним и тем же потоком
* ленивые - локи, которые не освобождаются потоком сразу после выхода из критической секции

При этом толстые локи могут со временем превращаться в тонкие и наоборот. Очевидно, для различных подходов к синхронизации  используются различные алгоритмы и комбинации локов, что ведет к различному поведению. В блоге Руслана Черемина(https://dev.cheremin.info) есть несколько статей, которые пытаются пролить на это свет.

В качестве резюме я бы сформулировал следующее утверждение: исследование выполненно довольно поверхностно и не претендует на истину в последней инстанции, но очевидно показывает, что утверждать, что использование synchronized в общем случае предпочтительнее с точки зрения производительности, чем java.util.concurrent.locks.Lock, нельзя.

Исходный код и результаты доступны по адресу: https://github.com/yozh1k/lockbenchmark

Запусить тесты можно перейдя в корень проекта:

```bash
mvn clean install
java -jar target/benchmarks.jar

```
