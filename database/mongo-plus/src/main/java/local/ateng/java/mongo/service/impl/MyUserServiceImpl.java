package local.ateng.java.mongo.service.impl;

import com.mongoplus.service.impl.ServiceImpl;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.service.MyUserService;
import org.springframework.stereotype.Service;

@Service
public class MyUserServiceImpl extends ServiceImpl<MyUser> implements MyUserService {
}
