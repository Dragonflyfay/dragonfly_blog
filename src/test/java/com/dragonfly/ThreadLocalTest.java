package com.dragonfly;

import org.junit.jupiter.api.Test;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/8 23:01
 */
public class ThreadLocalTest {
@Test
    public void testThreadLocalSetAndGet(){
        //提供一个ThreadLocal对象
        ThreadLocal tl=new ThreadLocal();

        //开启两个线程,传递线程任务和名字
        new Thread( ()->{
            tl.set("小樱");
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
        },"线程1"

        ).start();

        new Thread( ()->{
            tl.set("李小狼");
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
            System.out.println(Thread.currentThread().getName()+": "+tl.get());
        },"线程2"

        ).start();
    }
}
