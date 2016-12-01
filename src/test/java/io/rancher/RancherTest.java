package io.rancher;

import io.rancher.service.EnvironmentService;
import io.rancher.service.ServiceService;
import io.rancher.type.Environment;
import io.rancher.type.LaunchConfig;
import io.rancher.type.Service;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;

import java.net.URL;

/**
 * Rancher Client测试类
 * 测试前注意修改accessKey, secretKey
 * Created by xausky on 11/30/16.
 */
public class RancherTest {
    private Rancher rancher = null;
    private Environment environment = null;
    private EnvironmentService environmentService = null;

    /**
     * 测试前添加一个Environment也就是UI中的Stack
     * @throws Exception 发生异常
     */
    @Before
    public void before() throws Exception{
        Rancher.Config config = new Rancher.Config(
                new URL("http://172.17.0.2:8080/v1/"),
                "CFC4C18BEF5D2D6DEEA3",
                "Do1y93u8jMZ6s6LwjR4t7LF4Rvxi1AhtjJeVHsSn");
        rancher = new Rancher(config);
        environmentService = rancher.type(EnvironmentService.class);
        environment = new Environment();
        environment.setName("Test-" + System.currentTimeMillis());
        environment = environmentService.create(environment).execute().body();
    }

    /**
     * 测试完成后删除Environment
     * @throws Exception 发生异常
     */
    @After
    public void after() throws Exception{
        Assert.assertTrue(environmentService.remove(environment.getId()).execute().isSuccessful());
    }

    /**
     * 测试Environment是否正常
     * @throws Exception 发生异常
     */
    @Test
    public void environmentTest() throws Exception{
        Assert.assertEquals(environment.getHealthState(),"healthy");
    }

    /**
     * 在测试Stack里面创建一个redis服务并且确认正常启动，然后删除。
     * 测试前先使用 docker pull redis 下载好镜像，否则可能120秒后超时。
     * @throws Exception 发生异常
     */
    @Test
    public void serviceTest() throws Exception{
        ServiceService serviceService = rancher.type(ServiceService.class);
        LaunchConfig config = new LaunchConfig();
        config.setImageUuid("docker:redis");
        Service service = new Service();
        service.setName("redis");
        service.setEnvironmentId(environment.getId());
        service.setLaunchConfig(config);
        Response<Service> serviceResponse = serviceService.create(service).execute();
        Assert.assertTrue(serviceResponse.isSuccessful());
        service = serviceResponse.body();
        boolean active = false;
        int count = 120;
        while (count>0){
            System.out.println("state:"+service.getState());
            if(service.getState().equals("inactive") && !active){
                Assert.assertTrue(serviceService.activate(service.getId()).execute().isSuccessful());
                active = true;
            }
            if(service.getState().equals("active")){
                break;
            }
            service = serviceService.get(service.getId()).execute().body();
            Thread.sleep(1000);
            count--;
        }
        Assert.assertTrue(serviceService.remove(service.getId()).execute().isSuccessful());
    }
}
