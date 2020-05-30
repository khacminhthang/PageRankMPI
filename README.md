# Pagerank_MPJ
Thực thi thuật toán pagerank trên môi trường song song, sử dụng MPJ
B1.Import thư viện MPJ trong thư mục mpj-v_0.44
B2.Import project vào esclipe
# Thực hiện pagerank tuần tự
B3.Chạy file Pagerank.java
B4.Mở thư mục OutputFile xem kết quả
# Thực hiện pagerank song song với 4 tiến trình
B3.Run -> Run Configurations 
  Tab Arguments:
    - Trong mục VM arguments, chọn Variables -> Edit Variables -> New
      Name = MPJ_HOME
      Value = đường dẫn thư mục thư viện mpj-v0_44
     -> Apply
    - Nhập ${build_files}-jar ${MPJ_HOME}/lib/starter.jar -np N trong ô VM arguments
  Tab Enviroment:
    New:
      Name = MPJ_HOME
      Value = Variables -> MPJ_HOME
  Run
B4.Mở thư mục OutputFile xem kết quả trong file pagerankMPJ_10k.txt
