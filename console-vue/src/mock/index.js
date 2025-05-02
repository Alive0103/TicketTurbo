import Mock from 'mockjs'
Mock.mock('/api/login', 'get', () => {
  return {
    status: 10000,
    data: null,
    msg: '登录成功'
  }
})
