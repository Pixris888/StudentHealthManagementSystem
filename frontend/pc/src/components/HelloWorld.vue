<script>
import {request} from "../util/request.js";
export default {
  data(){
    return{
      name:"zhangzhou",
      list:[],
      message:'练习前端'
    }
  },
  methods:{
    async changeName(){
     const answer=await request('system/user/page','get',{
        page:'1',
        size:'10'
      })
      this.list=answer.data.records
    }
  },
  created() {
    this.changeName()
  }
}
</script>

<template>
  <h1>{{name}}</h1>
  <button @click="changeName">changeName</button>
  <ul>
    <li v-for="item in list" :key="item.id">{{item.username}}</li>
  </ul>
  <p>{{message}}</p>
  <input v-model="message">
</template>
