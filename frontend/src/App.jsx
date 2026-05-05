import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './AuthContext';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Search from './pages/Search';
import BookDetail from './pages/BookDetail';
import Subject from './pages/Subject';
import Login from './pages/Login';
import MyLoans from './pages/MyLoans';
import Cart from './pages/Cart';
import AdminPanel from './pages/AdminPanel';

function App() {
  return (
    <AuthProvider>
      <Navbar />
      <main className="page-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/search" element={<Search />} />
          <Route path="/works/:workId" element={<BookDetail />} />
          <Route path="/subjects/:subject" element={<Subject />} />
          <Route path="/login" element={<Login />} />
          <Route path="/loans" element={<MyLoans />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/admin" element={<AdminPanel />} />
        </Routes>
      </main>
    </AuthProvider>
  );
}

export default App;
